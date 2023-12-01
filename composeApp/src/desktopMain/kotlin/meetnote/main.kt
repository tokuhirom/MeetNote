package meetnote
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking
import meetnote.cleanup.Mp3CleanupProcessor
import meetnote.config.ConfigRepository
import meetnote.openai.OpenAICustomizedClient
import meetnote.postprocess.PostProcessingResumer
import meetnote.postprocess.PostProcessor
import meetnote.ui.mainWindow
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem
import kotlin.math.max
import kotlin.math.min

fun main() {
    val logger = LoggerFactory.getLogger("main")

    logger.info("Start application...")

    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val dataRepository = DataRepository()
    val openAICustomizedClient = OpenAICustomizedClient(config.apiToken!!)
    val postProcessor = PostProcessor(openAICustomizedClient, config.mp3bitRate, config.rawSampleRate)
    val postProcessingResumer = PostProcessingResumer(dataRepository, postProcessor)
    postProcessingResumer.start()
    val postProcessExecutor = Executors.newSingleThreadExecutor()

    val recorder = Recorder(dataRepository, config)

    val windowNameCollector = WindowNameCollector()

    Mp3CleanupProcessor().run(dataRepository, config)

    val fileWatcher = FileWatcher(dataRepository.getDataDirectory())
    fileWatcher.start()

    application {
        var isRecording by remember { mutableStateOf(false) }
        var currentVolume: Int? by remember { mutableStateOf(null) }

        recorder.onVolumeChecked = {
            currentVolume = it
        }
        recorder.onStartRecording = {
            isRecording = true
        }
        recorder.onStopRecording = {targetPath ->
            isRecording = false

            postProcessExecutor.submit {
                runBlocking {
                    postProcessor.process(targetPath)
                }
            }
        }

        Tray(
            icon = TrayIcon(
                isRecording,
                currentVolume
            ),
            menu = {
                Item("MeetNote") {
                }

                Separator()

                var selectedMixer by remember { mutableStateOf(recorder.selectedMixer) }

                AudioSystem.getMixerInfo().filter {
                    // targetLine=Output, Speaker
                    // sourceLine=Input, Microphone
                    AudioSystem.getMixer(it).targetLineInfo.isNotEmpty()
                }.forEach { mixerInfo ->
                    // Note: RadioButtonItem is not available on desktop
                    Item(
                        if (mixerInfo.name == selectedMixer.name) {
                            "✔"
                        } else {
                            "  "
                        } + mixerInfo.name
                    ) {
                        logger.info("Selected mixer: $mixerInfo")
                        recorder.setMixer(mixerInfo)
                        configRepository.saveConfiguration(configRepository.loadSettings()
                            .copy(mixer = mixerInfo.name))
                        selectedMixer = mixerInfo
                    }
                }

                Separator()

                Item("Exit") {
                    exitApplication()
                }
            }
        )

        mainWindow(configRepository, logger, recorder, windowNameCollector, config, dataRepository, postProcessor, fileWatcher)
    }
}

class TrayIcon(
    private val isRecording: Boolean,
    private val currentVolume: Int? // [db]
) : Painter() {
    override val intrinsicSize = Size(40f, 40f)

    override fun DrawScope.onDraw() {
        if (isRecording) {
            // 20db のときを 20%, 100db のときを 100% になるように調整してみる。
            val heightPercent = max(min(currentVolume ?: 0, 100), 0) / 100.0f * intrinsicSize.height
            println("current=$currentVolume, hp=$heightPercent")
            drawRect(
                color = Color.Green,
                topLeft = Offset(0f, intrinsicSize.height - heightPercent),  // Draw from bottom up
                size = Size(intrinsicSize.width, heightPercent),
                style = Fill
            )
        } else {
            drawRect(
                color = Color.Gray,
                topLeft = Offset(0f, 10f),
                size = Size(15f, 25f)
            )
            drawRect(
                color = Color.Gray,
                topLeft = Offset(30f, 10f),
                size = Size(15f, 25f)
            )
        }
    }
}
