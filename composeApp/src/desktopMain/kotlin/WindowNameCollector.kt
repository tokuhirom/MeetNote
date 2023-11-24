import java.io.BufferedReader
import java.io.InputStreamReader

class WindowNameCollector {
    data class WindowState(val processName: String, val processId: String, val bundleId: String, val windowName: String)

    fun getWindowStateList(): List<WindowState> {
        val windowListString = getWindowListString()

        return windowListString.map {
            parseWindowState(it)
        }
    }

    private fun parseWindowState(input: String): WindowState {
        // Regex pattern for matching the input string.
        val pattern = """Process: (.*?), PID: (.*?), Bundle ID: (.*?), Window: (.*?)""".toRegex(RegexOption.DOT_MATCHES_ALL)

        // Try to find a match in the input string.
        val matchResult = pattern.matchEntire(input)

        if (matchResult != null) {
            val (processName, processId, bundleId, windowName) = matchResult.destructured
            return WindowState(processName, processId, bundleId, windowName)
        } else {
            throw RuntimeException("Failed to parse window state: '$input'")
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

        val lines = BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
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
}
