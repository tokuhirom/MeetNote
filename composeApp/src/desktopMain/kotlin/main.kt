import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aallam.openai.client.OpenAI
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import openai.OpenAICustomizedClient
import java.time.Duration
import java.util.concurrent.Executors
import javax.sound.sampled.AudioSystem


@OptIn(ExperimentalFoundationApi::class)
fun main() {
    println("Start application...")

    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val dataRepository = DataRepository()
    val openAICustomizedClient = OpenAICustomizedClient(config.apiToken!!)
    val postProcessor = PostProcessor(OpenAI(config.apiToken!!), openAICustomizedClient)
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
            var showWindowListWindow by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                Thread {
                    println("Starting WindowNameRecorderController...")

                    WindowNameRecorderController(
                        recorder,
                        windowNameCollector,
                        listOf(
                            WindowNamePattern("us.zoom.xos", "Zoom Meeting"),
                        ),
                        Duration.ofSeconds(1),
                        Duration.ofMinutes(30),
                    ).start()
                }.start()
            }

            if (showWindowListWindow) {
                var windowNameList : List<WindowNameCollector.WindowState> by remember { mutableStateOf(listOf()) }

                DialogWindow(onCloseRequest = {showWindowListWindow = false}, title = "Window List", resizable = true) {
                    LaunchedEffect(Unit) {
                        while (true) {
                            windowNameList = windowNameCollector.getWindowStateList().sortedByDescending {
                                it.bundleId
                            }
                            delay(3000)
                        }
                    }

                    Column {
                        Row {
                            Text(
                                "bundleId",
                                Modifier.padding(4.dp),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "windowName",
                                Modifier.padding(4.dp),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(windowNameList) { windowState ->
                                Divider()
                                Row {
                                    SelectionContainer {
                                        Text(
                                            windowState.bundleId,
                                            Modifier.padding(4.dp)
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    SelectionContainer {
                                        Text(
                                            windowState.windowName,
                                            Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row {
                            Spacer(Modifier.weight(1f))

                            Button(onClick = {
                                showWindowListWindow = false
                            }) {
                                Text("Close")
                            }
                        }
                    }
                }
            }

            mainApp.App()

            MenuBar {
                this.Menu("Misc") {
                    Item("Window List", onClick = {
                        println("Clicked...")
                        showWindowListWindow = true
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
