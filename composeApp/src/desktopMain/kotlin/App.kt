import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.WatchKey
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import androidx.compose.ui.window.Tray
import java.awt.SystemTray

data class LogEntry(val path: Path, val content: String)

class MainApp(private val dataRepository: DataRepository) {
    val logger = LoggerFactory.getLogger(javaClass)

    private fun loadLogs(): List<LogEntry> {
        return dataRepository.getRecentSummarizedFiles().map {
            LogEntry(it, it.readText())
        }
    }

    @Composable
    fun App() {
        MaterialTheme {
            var logs by remember { mutableStateOf(loadLogs()) }
            logger.info("Starting App!!")

            LaunchedEffect(Unit) {
                while(true) {
                    logs = loadLogs()
                    delay(1000)
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
                                    title = { "Delete Confirmation" },
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
                                Text(log.path.name)

                                Spacer(modifier = Modifier.weight(1f))

                                Button(onClick = {
                                    val process = ProcessBuilder(
                                        "code", log.path.toAbsolutePath().toString()
                                    ).start()
                                    process.waitFor()
                                }) {
                                    Text("Edit")
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

    fun deleteFileWithSameNameVtt(file: File) {
        val vttFilePath = file.absolutePath.replace(".md", ".vtt")
        val vttFile = Paths.get(vttFilePath)
        vttFile.deleteIfExists()
        file.delete()
    }
}
