package meetnote

import meetnote.config.Config
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine
import kotlin.math.log10
import kotlin.math.sqrt


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

    private val format = AudioFormat(config.rawSampleRate * 1000f, 16, 1, true, false)  // Mono, 16-bit, 16kHz, signed, little endian

    lateinit var onStartRecording: (Path) -> Unit
    lateinit var onStopRecording: (Path) -> Unit
    lateinit var onVolumeChecked: (Int) -> Unit

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

        val path = dataRepository.getNewRawFilePath()

        logger.info("Starting recording from ${selectedMixer.name}. path=$path")
        NotificationSender.sendMessage("Starting recording from ${selectedMixer.name}. path=$path")

        val line = AudioSystem.getMixer(selectedMixer).getLine(dataLineInfo) as TargetDataLine
        line.open(format)
        line.start()

        recordingState = RecordingState(line, path)
        recordingWriterExecutor.submit {
            var out = 0

            FileOutputStream(path.toFile()).use { fos ->
                val buffer = ByteArray(line.bufferSize)
                while (true) {
                    // Read data from the line
                    val bytesRead = line.read(buffer, 0, buffer.size)
                    if (bytesRead <= 0) break

                    // Calculate the volume based on the data and print it
                    val shortBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    var sum = 0.0
                    while (shortBuffer.hasRemaining()) {
                        val amptitude = shortBuffer.get().toDouble()
                        sum += amptitude * amptitude
                    }
                    val rms = sqrt(sum / bytesRead)
                    val db = 20.0 * log10(rms)
                    logger.debug("Volume: $db dB")
                    onVolumeChecked(db.toInt())

                    // Write the data to the temporary file
                    fos.write(buffer, 0, bytesRead)

                    out += bytesRead
                }
            }

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
