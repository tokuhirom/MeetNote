package meetnote.recordercontroller

import meetnote.Recorder
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.util.*

data class ProcessPattern(var processName: String, var cpuUsageThreshold: Double)

class HighCpuUsageRecorderController(
    private val recorder: Recorder,
    private val processPatterns: List<ProcessPattern>,
    private val measureInterval: Int = 3,
    private val maxRecordingDuration: Duration = Duration.ofMinutes(60),
) : RecorderController {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val cpuUsagesHistory: Queue<Boolean> = LinkedList()

    override fun start() {
        // 録音時間が規定時間を経過するか、Zoom ウィンドウが閉じるまで録音を続ける
        while (true) {
            try {
                if (recorder.inRecording()) { // in recording
                    if (!shouldRecording()) {
                        // 録音を終了する
                        recorder.stopRecording()
                    } else if (recorder.recordingDuration() > maxRecordingDuration) {
                        logger.info("Recording duration exceeds the limit. Stopping recording.")

                        // 規定時間を超過したので、一旦 close する。
                        recorder.stopRecording()

                        // そして再度録音を開始する。
                        recorder.startRecording()
                    }
                } else { // not recording
                    if (shouldRecording()) {
                        if (!recorder.inRecording()) {
                            logger.info("High CPU usage detected. Starting recording...")
                            recorder.startRecording()
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error in recording: $e", e)
            }

            Thread.sleep(1000)
        }
    }

    private fun shouldRecording(): Boolean {
        val cpuUsage = getCpuUsage()

        // Continue recording if any of the recent measurements exceeds the threshold.
        if (cpuUsagesHistory.size >= measureInterval) {
            cpuUsagesHistory.remove()
        }
        cpuUsagesHistory.add(cpuUsage)
        return cpuUsagesHistory.any { it }
    }

    private fun getCpuUsage(): Boolean {
        val processBuilder = ProcessBuilder("ps", "aux")
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.useLines { lines ->
            lines.forEach { line ->
                processPatterns.forEach { processPattern ->
                    if (line.contains(processPattern.processName)) {
                        val usage = line.split("\\s+".toRegex())[2].toDoubleOrNull()
                        if (usage != null && usage > processPattern.cpuUsageThreshold) {
                            logger.debug("${processPattern.processName} cpu usage is $usage. It's greater than ${processPattern.cpuUsageThreshold}")
                            return true
                        }
                    }
                }
            }
        }

        return false
    }
}
