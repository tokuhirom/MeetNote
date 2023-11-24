package meetnote
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem


fun main() {
    println("Start application...")

    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val dataRepository = DataRepository()
    val openAICustomizedClient = OpenAICustomizedClient(config.apiToken!!)
    val postProcessor = PostProcessor(OpenAI(config.apiToken!!), openAICustomizedClient, config.mp3bitRate)
    val postProcessingResumer = PostProcessingResumer(dataRepository, postProcessor)
    postProcessingResumer.start()
    val postProcessExecutor = Executors.newSingleThreadExecutor()

    val mainApp = MainApp(dataRepository)
    val recorder = Recorder(dataRepository)

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
                        println("Selected mixer: $mixerInfo")
                        recorder.setMixer(mixerInfo)
                        selectedMixer = mixerInfo
                    }
                }

                Separator()

                Item("Exit") {
                    exitApplication()
                }
            }
        )

        Window(onCloseRequest = ::exitApplication, title = "MeetNote", state = rememberWindowState()) {
            var showWindowListDialog by remember { mutableStateOf(false) }
            var showConfigurationDialog by remember { mutableStateOf(configRepository.loadSettings().apiToken.isNullOrBlank()) }

            LaunchedEffect(Unit) {
                Thread {
                    println("Starting WindowNameRecorderController...")

                    WindowNameRecorderController(
                        recorder,
                        windowNameCollector,
                        config.windowWatchConfig.windowPatterns,
                        watchInterval = config.windowWatchConfig.watchInterval,
                        maxRecordingDuration = config.maxRecordingDuration,
                    ).start()
                }.start()
            }

            if (showWindowListDialog) {
                windowListDialog(windowNameCollector) {
                    showWindowListDialog = false
                }
            }

            if (showConfigurationDialog) {
                configurationDialog(configRepository) {
                    showConfigurationDialog = false
                }
            }

            mainApp.app()

            MenuBar {
                this.Menu("Misc") {
                    Item("Window List", onClick = {
                        showWindowListDialog = true
                    })
                    Item("Configuration", shortcut = KeyShortcut(Key.Comma, meta = true), onClick = {
                        showConfigurationDialog = true
                    })
                }
            }
        }
    }
}

class TrayIcon(private val color: Color) : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(color)
    }
}