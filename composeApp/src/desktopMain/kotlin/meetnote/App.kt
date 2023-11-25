package meetnote

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class LogEntry(val path: Path, var content: String) {
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
    fun app() {
        MaterialTheme {
            var logs by remember { mutableStateOf(loadLogs()) }
            logger.info("Starting App!!")

            val job = rememberCoroutineScope().launch(Dispatchers.IO) {
                val path = dataRepository.getDataDirectory()
                CoroutineScope(Dispatchers.IO).launch {
                    startFileWatcher(path) {
                        logger.info("File changed. Reloading logs...")
                        logs = loadLogs()
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    logger.info("Stop watching file changes.")
                    job.cancel()
                }
            }

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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

                                var showEditWindow by remember { mutableStateOf(false) }
                                var content by remember { mutableStateOf(log.content) }
                                var isDirty by remember { mutableStateOf(false) }

                                Button(onClick = {
                                    content = log.path.readText()
                                    showEditWindow = true
                                }) {
                                    Text("Edit")
                                }

                                if (showEditWindow) {
                                    var showConfirmDialog by remember { mutableStateOf(false) }

                                    fun saveContent() {
                                        log.path.writeText(content)
                                        isDirty = false
                                        log.content = content
                                    }

                                    Window(
                                        onCloseRequest = { },
                                        title = "Editing ${log.title()}" + if (isDirty) { " (Editing)" } else { "" },
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
                                                    Item("Close", shortcut = KeyShortcut(Key.W, meta=true), onClick = {
                                                        if (isDirty) {
                                                            // Show "really close this window?" dialog
                                                            showConfirmDialog = true
                                                        } else {
                                                            showEditWindow = false
                                                        }
                                                    })
                                                }
                                            }
                                            if (showConfirmDialog) {
                                                Dialog(onDismissRequest = { showConfirmDialog = false }) {
                                                    Column(modifier = Modifier.background(Color.LightGray)) {
                                                        Text(text = "Really close the editing window？", modifier = Modifier.padding(8.dp))
                                                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                                                            Button(onClick = { showConfirmDialog = false; }) {
                                                                Text("Cancel")
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Button(onClick = {
                                                                showConfirmDialog = false
                                                                saveContent()
                                                                showEditWindow = false
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

    private fun deleteFileWithSameNameVtt(file: File) {
        val vttFilePath = file.absolutePath.replace(".md", ".vtt")
        val vttFile = Paths.get(vttFilePath)
        vttFile.deleteIfExists()
        file.delete()
    }

    private fun startFileWatcher(path: Path, callback: () -> Unit) {
        val watchService = FileSystems.getDefault().newWatchService()

        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        Thread {
            while (true) {
                val key = watchService.take()
                if (key.pollEvents().any { event ->
                        event.context() is Path && (event.context() as Path).toString().endsWith(".md")
                    }) {
                    callback()
                }
                key.reset()
            }
        }.start()
    }
}
