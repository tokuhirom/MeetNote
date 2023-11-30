package meetnote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
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

@Composable
private fun ApplicationScope.mainWindow(
    configRepository: ConfigRepository,
    logger: Logger,
    recorder: Recorder,
    windowNameCollector: WindowNameCollector,
    config: Config,
    dataRepository: DataRepository,
    postProcessor: PostProcessor
) {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MeetNote",
        state = rememberWindowState(),
        icon = painterResource("icons/icon.png")
    ) {

        LaunchedEffect(Unit) {
            Thread {
                logger.info("Starting WindowNameRecorderController...")

                WindowNameRecorderController(
                    recorder,
                    windowNameCollector,
                    config.windowWatchConfig.windowPatterns,
                    watchInterval = config.windowWatchConfig.watchInterval,
                    maxRecordingDuration = config.maxRecordingDuration,
                ).start()
            }.start()
        }

        mainWindowBody(postProcessor, dataRepository, windowNameCollector, configRepository)
    }
}

class TrayIcon(private val color: Color) : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(color)
    }
}
