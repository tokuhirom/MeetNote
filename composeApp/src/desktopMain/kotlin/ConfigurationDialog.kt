import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindow

@Composable
fun configurationDialog(configRepository: ConfigRepository, onClose: () -> Unit) {
    DialogWindow(onClose, title = "Configuration", resizable = true) {
        Column {
            var apiToken by remember { mutableStateOf(configRepository.loadSettings().apiToken) }

            TextField(
                value = apiToken ?: "",
                onValueChange = {
                    apiToken = it
                },
                label = {
                    Text("API Token")
                }
            )

            Button(
                onClick = {
                    configRepository.saveSettings(
                        Config(
                            apiToken = apiToken
                        )
                    )
                    onClose()
                }
            ) {
                Text("Save")
            }
        }
    }
}
