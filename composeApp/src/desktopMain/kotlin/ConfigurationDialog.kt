import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindow
import org.slf4j.LoggerFactory
import java.time.Duration

@Composable
fun configurationDialog(configRepository: ConfigRepository, onClose: () -> Unit) {
    val logger = LoggerFactory.getLogger("configurationDialog")

    DialogWindow(onClose, title = "Configuration", resizable = true) {
        Column {
            var config by remember { mutableStateOf(configRepository.loadSettings()) }

            TextField(
                value = config.apiToken ?: "",
                onValueChange = {
                    config.apiToken = it
                },
                label = {
                    Text("API Token")
                }
            )

            Divider()

            var sleepIntervalIsError  by remember { mutableStateOf(false) }

            TextField(
                value = config.recorderControllerConfig.sleepInterval.toSeconds().toString(),
                onValueChange = { value ->
                    logger.info("Updating sleep Interval: $value")
                    val longValue = value.toLongOrNull()
                    if (longValue != null) {
                        config = config.copy(
                            recorderControllerConfig = config.recorderControllerConfig.copy(
                                sleepInterval = Duration.ofSeconds(longValue)
                            )
                        )
                        sleepIntervalIsError = false
                    } else {
                        sleepIntervalIsError = true
                    }
                },
                isError = sleepIntervalIsError,
                label = {
                    Text("Sleep Interval [min]")
                }
            )

//            var maxRecordingDuration: Duration = Duration.ofMinutes(30),

            Button(
                onClick = {
                    configRepository.saveConfiguration(config)
                    onClose()
                }
            ) {
                Text("Save")
            }
        }
    }
}
