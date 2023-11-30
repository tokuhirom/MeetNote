package meetnote

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class LogEntry(val path: Path) {
    val content: String by lazy {
        path.readText()
    }

    val vttPath : Path by lazy {
        path.resolveSibling(path.name.replace(".md", ".vtt"))
    }

    val mp3Path : Path by lazy {
        path.resolveSibling(path.name.replace(".md", ".mp3"))
    }

    fun title(): String {
        val inputFileName = path.name

        val datePart = inputFileName.substring(0, 8)  // "20230102"
        val timePart = inputFileName.substring(8, 12)  // "0304"

        val formattedDate = datePart.replaceRange(4, 4, "-").replaceRange(7, 7, "-")
        val formattedTime = timePart.replaceRange(2, 2, ":")

        return "$formattedDate $formattedTime"
    }
}

class MainApp(private val dataRepository: DataRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun loadLogs(): List<LogEntry> {
        return dataRepository.getRecentSummarizedFiles().map {
            LogEntry(it, it.readText())
        }
    }

    @Composable
    fun app(postProcessor: PostProcessor) {
        MaterialTheme {
            var logs by remember { mutableStateOf(loadLogs()) }
            logger.info("Starting App!!")

            val job = rememberCoroutineScope().launch(Dispatchers.IO) {
                val path = dataRepository.getDataDirectory()
                logger.info("Starting file watching coroutine...")

                startFileWatcher(path) {
                    logger.info("File changed. Reloading logs...")
                    logs = loadLogs()
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    logger.info("Stop watching file changes.")
                    job.cancel()
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (postProcessor.state.isNotBlank()) {
                    Text(postProcessor.state, color = Color.Blue)
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(logs) {log ->
                        if (!log.path.isRegularFile()) {
                            return@items
                        }

                        Column(modifier = Modifier.padding(4.dp)) {
                            var isConfirmDialogOpen by remember { mutableStateOf(false) }

                            if (isConfirmDialogOpen) {
                                AlertDialog(
                                    onDismissRequest = { isConfirmDialogOpen = false },
                                    title = { Text("Delete Confirmation") },
                                    text = { Text("Are you sure you want to delete ${log.path.name}?") },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                deleteFileWithSameNameVtt(log.path.toFile())
                                                isConfirmDialogOpen = false
                                            }
                                        ) {
                                            Text("Yes")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { isConfirmDialogOpen = false }
                                        ) {
                                            Text("No")
                                        }
                                    }
                                )
                            }

                            Row {
                                Text(log.title())

                                Spacer(modifier = Modifier.weight(1f))

                                var showEditSummaryWindow by remember { mutableStateOf(false) }

                                Button(onClick = {
                                    showEditSummaryWindow = true
                                }) {
                                    Text("Edit Summary")
                                }

                                if (showEditSummaryWindow) {
                                    editSummaryWindow(log) {
                                        showEditSummaryWindow = false
                                    }
                                }

                                if (log.vttPath.exists()) {
                                    var showVttWindow by remember { mutableStateOf(false) }
                                    Button(onClick = {
                                        showVttWindow = true
                                    }) {
                                        Text("View raw log(VTT)")
                                    }
                                    if (showVttWindow) {
                                        vttWindow(log) {
                                            showVttWindow = false
                                        }
                                    }
                                }

                                Button(onClick = {
                                    isConfirmDialogOpen = true
                                }) {
                                    Text("Delete")
                                }
                            }


                            SelectionContainer {
                                Text(log.content)
                            }
                        }

                        Divider()
                    }
                }
            }
        }
    }
}

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
                        Text(if (playing) {
                            "⏸"
                        } else {
                            "▶"
                        })
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

@Composable
fun editSummaryWindow(
    log: LogEntry,
    onHideWindow: () -> Unit,
) {
    val logger = LoggerFactory.getLogger("editSummaryWindow")

    var showConfirmDialog by remember { mutableStateOf(false) }

    var content by remember { mutableStateOf(log.content) }
    var isDirty by remember { mutableStateOf(false) }
    fun saveContent() {
        log.path.writeText(content)
        isDirty = false
        log.content = content
    }

    Window(
        onCloseRequest = { },
        title = "Editing ${log.title()}" + if (isDirty) {
            " (Editing)"
        } else {
            ""
        },
        content = {
            TextField(
                value = content,
                onValueChange = {
                    logger.info("onValueChange: $it")
                    content = it
                    isDirty = true
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
            MenuBar {
                this.Menu("File") {
                    Item("Save", shortcut = KeyShortcut(Key.S, meta = true), onClick = {
                        logger.info("Saving ${log.path}")
                        saveContent()
                    })
                    Item("Close", shortcut = KeyShortcut(Key.W, meta = true), onClick = {
                        if (isDirty) {
                            // Show "really close this window?" dialog
                            showConfirmDialog = true
                        } else {
                            onHideWindow()
                        }
                    })
                }
            }
            if (showConfirmDialog) {
                Dialog(onDismissRequest = { showConfirmDialog = false }) {
                    Column(modifier = Modifier.background(Color.LightGray)) {
                        Text(text = "Really close the editing window？", modifier = Modifier.padding(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = { showConfirmDialog = false; }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                showConfirmDialog = false
                                saveContent()
                                onHideWindow()
                            }) {
                                Text("Save and Close")
                            }
                        }
                    }
                }
            }
        }
    )
}

fun deleteFileWithSameNameVtt(file: File) {
    val vttFilePath = file.absolutePath.replace(".md", ".vtt")
    val vttFile = Paths.get(vttFilePath)
    vttFile.deleteIfExists()
    file.delete()
}

fun startFileWatcher(path: Path, callback: () -> Unit) {
    val logger = LoggerFactory.getLogger("startFileWatcher")
    val watchService = FileSystems.getDefault().newWatchService()

    path.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE
    )

    Thread {
        logger.info("Starting file watching thread...: $path")

        while (true) {
            val key = watchService.take()
            logger.info("Got watch event.")
            val events = key.pollEvents()
            for (event in events) {
                logger.info("Event: context=${event.context()}")
                callback()
                break
            }
            key.reset()
        }
    }.start()
}
