package meetnote.recordercontroller

import meetnote.Recorder
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Duration
import java.util.*

data class ProcessPattern(var processName: String, var cpuUsageThreshold: Double)

data class RecordingReason(val processPattern: ProcessPattern, val cpuUsage: Double)

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
                    val (shouldRecord, _) = shouldRecording()
                    if (!shouldRecord) {
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
                    val (shouldRecord, reason) = shouldRecording()
                    if (shouldRecord) {
                        if (!recorder.inRecording()) {
                            logger.info("High CPU usage detected. Starting recording...: $reason")
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

    private fun shouldRecording(): Pair<Boolean, RecordingReason?> {
        val (cpuUsage, reason) = getCpuUsage()

        // Continue recording if any of the recent measurements exceeds the threshold.
        if (cpuUsagesHistory.size >= measureInterval) {
            cpuUsagesHistory.remove()
        }
        cpuUsagesHistory.add(cpuUsage)
        return cpuUsagesHistory.any { it } to reason
    }

    private fun getCpuUsage(): Pair<Boolean, RecordingReason?> {
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
                            return true to RecordingReason(processPattern, usage)
                        }
                    }
                }
            }
        }

        return false to null
    }
}
