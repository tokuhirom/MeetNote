import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration

data class WindowNamePattern(val bundleId: String, val windowName: String)
data class WindowState(val processName: String, val processId: String, val bundleId: String, val windowName: String)

class WindowNameRecorderController(
    private val recorder: Recorder,
    private val windowNamePatterns: List<WindowNamePattern>,
    private val sleepInterval: Duration,
    private val maxRecordingDuration: Duration = Duration.ofMinutes(30),
) : RecorderController {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class WindowState(val processName: String, val processId: String, val bundleId: String, val windowName: String)

    private fun parseWindowState(input: String): WindowState {
        // Regex pattern for matching the input string.
        val pattern = """Process: (.*), PID: (.*), Bundle ID: (.*), Window: (.*)""".toRegex()

        // Try to find a match in the input string.
        val matchResult = pattern.matchEntire(input)

        if (matchResult != null) {
            val (processName, processId, bundleId, windowName) = matchResult.destructured
            return WindowState(processName, processId, bundleId, windowName)
        } else {
            throw RuntimeException("Failed to parse window state: '$input'")
        }
    }

    fun getWindowStateList(): List<WindowState> {
        val windowListString = getWindowListString()

        return windowListString.map {
            parseWindowState(it)
        }
    }

    private fun getWindowListString(): List<String> {
        val script = """
tell application "System Events"
    set procs to processes
    set results to ""
    repeat with proc in procs
        if exists (window 1 of proc) then
            repeat with w in windows of proc
                set results to results & "Process: " & name of proc & ", PID: " & unix id of proc & ", Bundle ID: " & bundle identifier of proc & ", Window: " & name of w & "\n"
            end repeat
        end if
    end repeat
    return results
end tell
        """.trimIndent()

        val pb = ProcessBuilder("osascript", "-e", script)
        val p = pb.start()

        val lines = BufferedReader(InputStreamReader(p.inputStream)).use {reader ->
            reader.readLines().filter {
                it.isNotBlank()
            }
        }

        val exitStatus = p.waitFor() // Wait for the process to finish.
        if (exitStatus != 0) {
            throw RuntimeException("Failed to execute osascript. Exit status: $exitStatus")
        }

        return lines
    }

    override fun start() {
        while (true) {
            try {
                val matchedWindowList = getWindowStateList()
                    .filter {
                        windowNamePatterns.any { pattern ->
                            it.bundleId == pattern.bundleId && it.windowName == pattern.windowName
                        }
                    }

                if (matchedWindowList.isNotEmpty()) {
                    if (!recorder.inRecording()) {
                        logger.info("Matched window found. Starting recording...")
                        recorder.startRecording()
                    } else if (recorder.recordingDuration() > maxRecordingDuration) {
                        logger.info("Recording duration exceeds the limit. Stopping recording.")

                        // 規定時間を超過したので、一旦 close する。
                        recorder.stopRecording()

                        // そして再度録音を開始する。
                        recorder.startRecording()
                    }
                } else {
                    if (recorder.inRecording()) {
                        logger.info("Matched window not found. Stopping recording...")
                        recorder.stopRecording()
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in recording: $e", e)
            }

            Thread.sleep(sleepInterval.toMillis())
        }
    }
}
