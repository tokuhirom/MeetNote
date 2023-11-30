package meetnote.recordercontroller

import meetnote.Recorder
import meetnote.WindowNameCollector
import org.slf4j.LoggerFactory
import java.time.Duration

data class WindowPattern(var bundleId: String, var windowName: String)

class WindowNameRecorderController(
    private val recorder: Recorder,
    private val windowNameCollector: WindowNameCollector,
    private val windowPatterns: List<WindowPattern>,
    private val watchInterval: Duration,
    private val maxRecordingDuration: Duration = Duration.ofMinutes(30),
) : RecorderController {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun start() {
        while (true) {
            try {
                val matchedWindowList = windowNameCollector.getWindowStateList()
                    .filter {
                        windowPatterns.any { pattern ->
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

            Thread.sleep(watchInterval.toMillis())
        }
    }
}
