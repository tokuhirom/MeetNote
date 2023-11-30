package meetnote
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.runBlocking
import meetnote.config.ConfigRepository
import meetnote.openai.OpenAICustomizedClient
import meetnote.postprocess.PostProcessingResumer
import meetnote.postprocess.PostProcessor
import meetnote.ui.mainWindow
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem

fun main() {
    val logger = LoggerFactory.getLogger("main")

    logger.info("Start application...")

    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val dataRepository = DataRepository()
    val openAICustomizedClient = OpenAICustomizedClient(config.apiToken!!)
    val postProcessor = PostProcessor(OpenAI(config.apiToken!!), openAICustomizedClient, config.mp3bitRate)
    val postProcessingResumer = PostProcessingResumer(dataRepository, postProcessor)
    postProcessingResumer.start()
    val postProcessExecutor = Executors.newSingleThreadExecutor()

    val recorder = Recorder(dataRepository, config)

    val windowNameCollector = WindowNameCollector()

    application {
        var isRecording by remember { mutableStateOf(false) }

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
                if (isRecording) {
                    Color(0xFFFF0000)
                } else {
                    Color.Gray
                }
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

        mainWindow(configRepository, logger, recorder, windowNameCollector, config, dataRepository, postProcessor)
    }
}

class TrayIcon(private val color: Color) : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(color)
    }
}
