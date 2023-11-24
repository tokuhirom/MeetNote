import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import kotlinx.coroutines.delay

@Composable
fun windowListDialog(windowNameCollector: WindowNameCollector, onCloseRequest: () -> Unit) {
    var windowNameList : List<WindowNameCollector.WindowState> by remember { mutableStateOf(listOf()) }

    DialogWindow(onCloseRequest, title = "Window List", resizable = true) {
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
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "windowName",
                    Modifier.padding(4.dp),
                    fontWeight = FontWeight.Bold
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

                Button(onClick = onCloseRequest) {
                    Text("Close")
                }
            }
        }
    }
}
