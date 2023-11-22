import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine

class Recorder(
    private val dataRepository: DataRepository, private val onStartRecording: (Path) -> Unit = {},
    private val onStopRecording: (Path) -> Unit
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val format = AudioFormat(16000.0f, 16, 2, true, true)

    private val availableMixers = AudioSystem.getMixerInfo()

    var selectedMixer: Mixer.Info = availableMixers.firstOrNull {
        it.name.equals("Aggregate Device")
    } ?: availableMixers.first()
    private val dataLineInfo = DataLine.Info(TargetDataLine::class.java, format)

    private val recordingControllerExecutor = Executors.newSingleThreadExecutor()
    private val recordingWriterExecutor = Executors.newSingleThreadExecutor()

    private val maxRecordingDuration = Duration.ofMinutes(30)

    private var startedAt : Instant? = null
    private var line : TargetDataLine? = null
    private var path: Path? = null

    private fun startRecording() {
        path = dataRepository.getNewWaveFilePath()

        // TODO notice を出したい。
        logger.info("Starting recording from ${selectedMixer.name}. path=$path")

        if (line != null) {
            throw IllegalStateException("line is already initialized")
        }
        startedAt = Instant.now()
        line = AudioSystem.getMixer(selectedMixer).getLine(dataLineInfo) as TargetDataLine
        line!!.open(format)
        line!!.start()
        recordingWriterExecutor.submit {
            val out = AudioSystem.write(AudioInputStream(line), AudioFileFormat.Type.WAVE, path!!.toFile())
            println("Wrote $path: $out bytes")
        }

        onStartRecording(path!!)
    }

    private fun inRecording() : Boolean {
        return line != null && startedAt != null && path != null
    }

    fun start() {
        recordingControllerExecutor.submit {
            // 録音時間が規定時間を経過するか、Zoom ウィンドウが閉じるまで録音を続ける
            while (true) {
                try {
                    if (inRecording()) { // in recording
                        if (!inMeeting()) {
                            // 録音を終了する
                            stopRecording()
                        } else if (longerThanMaxRecordingDuration()) {
                            // 規定時間を超過したので、一旦 close する。
                            stopRecording()

                            // そして再度録音を開始する。
                            startRecording()
                        }
                    } else { // not recording
                        if (inMeeting()) {
                            // 録音を開始する
                            startRecording()
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error in recording: $e", e)
                }

                Thread.sleep(1000)
            }
        }
    }

    private fun inMeeting(): Boolean {
        val cpuUsage = getZoomCpuUsage()
        return cpuUsage != null && cpuUsage > 10.0
    }

    private fun getZoomCpuUsage(): Double? {
        val processBuilder = ProcessBuilder("ps", "aux")
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.useLines { lines ->
            lines.forEach { line ->
                if (line.contains("/Applications/zoom.us.app/Contents/MacOS/zoom.us")) {
                    return line.split("\\s+".toRegex())[2].toDoubleOrNull()
                }
            }
        }

        return null
    }

    private fun stopRecording() {
        line!!.stop();
        line!!.close();
        line = null
        startedAt = null
        val targetPath = path!!.toAbsolutePath()

        onStopRecording(targetPath)
    }

    private fun longerThanMaxRecordingDuration(): Boolean {
        val now = Instant.now()

        return Duration.ofSeconds(now.epochSecond - startedAt!!.epochSecond) > maxRecordingDuration
    }

    fun setMixer(mixerInfo: Mixer.Info) {
        stopRecording()
        selectedMixer = mixerInfo
        startRecording()
    }
}
