package meetnote

import org.slf4j.LoggerFactory
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


data class RecordingState(
    val line: TargetDataLine,
    val path: Path,
    val startedAt: Instant = Instant.now()
)

class Recorder(
    private val dataRepository: DataRepository,
    config: Config,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val format = AudioFormat(16000.0f, 16, 2, true, true)

    lateinit var onStartRecording: (Path) -> Unit
    lateinit var onStopRecording: (Path) -> Unit

    private val availableMixers = AudioSystem.getMixerInfo()

    var selectedMixer: Mixer.Info = if (!config.mixer.isNullOrEmpty()) {
        availableMixers.firstOrNull {
            it.name.equals(config.mixer)
        } ?: availableMixers.first()
    } else {
        availableMixers.first()
    }
    private val dataLineInfo = DataLine.Info(TargetDataLine::class.java, format)

    private val recordingWriterExecutor = Executors.newSingleThreadExecutor()

    private var recordingState: RecordingState? = null

    fun recordingDuration(): Duration {
        val now = Instant.now()

        return Duration.ofMillis(now.toEpochMilli() - recordingState!!.startedAt.toEpochMilli())
    }

    fun startRecording() {
        logger.info("BEGIN: startRecording")
        if (inRecording()) {
            stopRecording()
        }

        val path = dataRepository.getNewWaveFilePath()

        logger.info("Starting recording from ${selectedMixer.name}. path=$path")
        NotificationSender.sendMessage("Starting recording from ${selectedMixer.name}. path=$path")

        val line = AudioSystem.getMixer(selectedMixer).getLine(dataLineInfo) as TargetDataLine
        line.open(format)
        line.start()

        recordingState = RecordingState(line, path)
        recordingWriterExecutor.submit {
            val out = AudioSystem.write(AudioInputStream(line), AudioFileFormat.Type.WAVE, path.toFile())
            logger.info("Wrote $path: $out bytes")
        }

        onStartRecording(path)
        logger.info("END: startRecording")
    }

    fun inRecording() : Boolean {
        return recordingState != null
    }

    fun stopRecording() {
        logger.info("Stop recording: $recordingState")
        NotificationSender.sendMessage("Stop recording: $recordingState")

        val state = recordingState
        if (state != null) {
            state.line.stop();
            state.line.close();

            val targetPath = state.path.toAbsolutePath()
            onStopRecording(targetPath)

            recordingState = null
        }
    }

    fun setMixer(mixerInfo: Mixer.Info) {
        if (inRecording()) {
            stopRecording()
            selectedMixer = mixerInfo
            startRecording()
        } else {
            selectedMixer = mixerInfo
        }
    }
}
