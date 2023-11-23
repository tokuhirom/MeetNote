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



class Recorder(
    private val dataRepository: DataRepository,
    private val onStartRecording: (Path) -> Unit = {},
    private val onStopRecording: (Path) -> Unit
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val format = AudioFormat(16000.0f, 16, 2, true, true)

    private val availableMixers = AudioSystem.getMixerInfo()

    var selectedMixer: Mixer.Info = availableMixers.firstOrNull {
        it.name.equals("Aggregate Device")
    } ?: availableMixers.first()
    private val dataLineInfo = DataLine.Info(TargetDataLine::class.java, format)

    private val recordingWriterExecutor = Executors.newSingleThreadExecutor()

    private var startedAt : Instant? = null
    private var line : TargetDataLine? = null
    private var path: Path? = null

    fun recordingDuration(): Duration {
        val now = Instant.now()

        return Duration.ofMillis(now.toEpochMilli() - startedAt!!.toEpochMilli())
    }

    fun startRecording() {
        if (inRecording()) {
            stopRecording()
        }

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

    fun inRecording() : Boolean {
        return line != null && startedAt != null && path != null
    }

    fun stopRecording() {
        logger.info("Stop recording: $path")
        line!!.stop();
        line!!.close();
        line = null
        startedAt = null
        val targetPath = path!!.toAbsolutePath()

        onStopRecording(targetPath)
        path = null
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
