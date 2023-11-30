package meetnote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import meetnote.model.LogEntry
import org.slf4j.LoggerFactory
import kotlin.io.path.writeText

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
        log.reloadContent()
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
