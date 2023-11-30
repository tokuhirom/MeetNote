package meetnote.ui

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
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import meetnote.DataRepository
import meetnote.WindowNameCollector
import meetnote.config.ConfigRepository
import meetnote.deleteFileWithSameNameVtt
import meetnote.postprocess.PostProcessor
import meetnote.startFileWatcher
import meetnote.windowListDialog
import org.slf4j.LoggerFactory
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

@Composable
fun FrameWindowScope.mainWindowBody(postProcessor: PostProcessor, dataRepository: DataRepository, windowNameCollector: WindowNameCollector, configRepository: ConfigRepository) {
    val logger = LoggerFactory.getLogger("mainWindowBody")

    var showWindowListDialog by remember { mutableStateOf(false) }
    var showConfigurationDialog by remember { mutableStateOf(configRepository.loadSettings().apiToken.isNullOrBlank()) }

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

    MaterialTheme {
        var logs by remember { mutableStateOf(dataRepository.getRecentSummarizedLogs()) }
        logger.info("Starting App!!")

        val job = rememberCoroutineScope().launch(Dispatchers.IO) {
            val path = dataRepository.getDataDirectory()
            logger.info("Starting file watching coroutine...")

            startFileWatcher(path) {
                logger.info("File changed. Reloading logs...")
                logs = dataRepository.getRecentSummarizedLogs()
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
                items(logs) { log ->
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
