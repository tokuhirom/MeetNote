package meetnote.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import kotlinx.coroutines.delay
import meetnote.compactionWebVtt
import meetnote.model.LogEntry
import meetnote.parseWebVtt
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun vttWindow(log: LogEntry, onHideWindow: () -> Unit) {
    val logger = LoggerFactory.getLogger("vttWindow")

    Window(
        onCloseRequest = { onHideWindow() },
        title = "Viewing ${log.title()}(VTT)"
    ) {
        Column {
            val tmpFile = Files.createTempFile("meetnote", ".mp3")
            var audioInputStream: AudioInputStream? by remember { mutableStateOf(null) }
            var clip: Clip? by remember { mutableStateOf(null) }
            var playing by remember { mutableStateOf(false) }
            var seekToMicroSecond: Long? by remember { mutableStateOf(null) }
            var currentPosition: String? by remember { mutableStateOf(null) }
            val hasMp3File = log.mp3Path.exists()

            LaunchedEffect(playing, seekToMicroSecond) {
                if (playing) {
                    while (clip == null) {
                        logger.info("Waiting clip to ready")
                        delay(1000)
                    }
                    clip?.microsecondPosition = seekToMicroSecond ?: 0
                    clip?.stop()
                    clip?.start()
                } else {
                    val p = clip?.microsecondPosition
                    if (p != null && p != seekToMicroSecond) {
                        seekToMicroSecond = p
                    }
                    clip?.stop()
                }
            }

            LaunchedEffect(Unit) {
                if (clip == null) {
                    val mp3Path = log.mp3Path

                    logger.info("Converting $mp3Path to wave file")
                    val processBuilder = ProcessBuilder(
                        "lame",
                        "--quiet",
                        "--decode",
                        mp3Path.toAbsolutePath().toString(),
                        tmpFile.toAbsolutePath().toString(),
                    )
                    processBuilder.redirectError()
                    val process = processBuilder.start()
                    val mpg123log = String(process.inputStream.readAllBytes())
                    val exitCode = process.waitFor()
                    if (exitCode != 0) {
                        logger.error("Cannot convert mp3 to wave file: $mpg123log")
                    } else {
                        logger.info("Converted $mp3Path to $tmpFile")
                    }

                    audioInputStream = AudioSystem.getAudioInputStream(tmpFile.toFile())
                    clip =
                        AudioSystem.getLine(DataLine.Info(Clip::class.java, audioInputStream!!.format)) as Clip
                    clip!!.open(audioInputStream)
                }
            }
            LaunchedEffect(Unit) {
                while (true) {
                    val p = clip?.microsecondPosition
                    currentPosition = if (p != null) {
                        LocalTime.ofNanoOfDay(p * 1000).toString()
                    } else {
                        null
                    }
                    delay(1000)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    audioInputStream?.close()
                    tmpFile.deleteIfExists()
                }
            }

            if (hasMp3File) {
                Row {
                    Button(onClick = {
                        playing = !playing
                    }) {
                        Text(
                            if (playing) {
                                "⏸"
                            } else {
                                "▶"
                            }
                        )
                    }

                    Text(currentPosition ?: "")
                }
            }

            LazyColumn {
                val content = compactionWebVtt(parseWebVtt(log.vttPath.readText()))
                items(content) { row ->
                    Row(modifier = Modifier.padding(4.dp)) {
                        if (hasMp3File) {
                            Text("▶", modifier = Modifier.onClick {
                                seekToMicroSecond = (row.start.toSecondOfDay() * 1000 * 1000).toLong()
                                playing = true
                            })
                        }

                        SelectionContainer {
                            Text(
                                row.start.format(DateTimeFormatter.ISO_TIME)
                                        + " " + row.end.format(DateTimeFormatter.ISO_TIME)
                                        + " " + row.content,
                            )
                        }
                    }
                }
            }
        }

        MenuBar {
            this.Menu("File") {
                Item("Close", shortcut = KeyShortcut(Key.W, meta = true), onClick = {
                    onHideWindow()
                })
            }
        }
    }
}
