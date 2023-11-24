import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import org.slf4j.LoggerFactory
import java.time.Duration
import androidx.compose.ui.Modifier
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

            Spacer(modifier = Modifier.height(8.dp))

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
                    Text("Sleep Interval [sec]")
                }
            )

            var maxRecordingDurationIsError  by remember { mutableStateOf(false) }
            TextField(
                value = config.recorderControllerConfig.maxRecordingDuration.toMinutes().toString(),
                onValueChange = { value ->
                    logger.info("Updating maxRecordingDuration: $value")
                    val longValue = value.toLongOrNull()
                    if (longValue != null) {
                        config = config.copy(
                            recorderControllerConfig = config.recorderControllerConfig.copy(
                                maxRecordingDuration = Duration.ofMinutes(longValue)
                            )
                        )
                        maxRecordingDurationIsError = false
                    } else {
                        maxRecordingDurationIsError = true
                    }
                },
                isError = maxRecordingDurationIsError,
                label = {
                    Text("Max recording duration [min]\n(MP3 file size limit is 25MB..\nIf OpenAPI returns an error, you need to decrease this value.)")
                }
            )

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
