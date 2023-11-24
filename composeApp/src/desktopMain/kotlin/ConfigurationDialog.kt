
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.slf4j.LoggerFactory
import java.time.Duration

@Composable
fun configurationDialog(configRepository: ConfigRepository, onClose: () -> Unit) {
    val logger = LoggerFactory.getLogger("configurationDialog")

    DialogWindow(
        onCloseRequest = onClose,
        title = "Configuration",
        resizable = true,
        state = rememberDialogState(width = 800.dp, height = 600.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            var config by remember { mutableStateOf(configRepository.loadSettings()) }

            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                Column {
                    Text("OpenAI Configuration")
                    TextField(
                        value = config.apiToken ?: "",
                        onValueChange = { config = config.copy(apiToken = it) },
                        label = { Text("API Token") }
                    )
                }
            }

            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                Column {
                    Text("Recorder Controller Configurations")
                    Button(
                        onClick = {
                            config = config.copy(
                                recorderControllerConfig = config.recorderControllerConfig.copy(
                                    windowNamePatterns = config.recorderControllerConfig.windowNamePatterns +
                                            WindowNamePattern("com.example", "Great new window")
                                )
                            )
                        },
                    ) {
                        Text("Add")
                    }

                    Box(modifier = Modifier.padding(top = 10.dp)) {
                        LazyColumn {
                            items(config.recorderControllerConfig.windowNamePatterns) { windowNamePattern ->
                                Row {
                                    TextField(
                                        value = windowNamePattern.bundleId,
                                        onValueChange = {value ->
                                            config = config.copy(
                                                recorderControllerConfig = config.recorderControllerConfig.copy(
                                                    windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.map {
                                                        if (it == windowNamePattern) {
                                                            it.copy(bundleId = value)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                )
                                            )
                                        },
                                        label = { Text("Bundle ID") },
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    TextField(
                                        value = windowNamePattern.windowName,
                                        onValueChange = {value ->
                                            config = config.copy(
                                                recorderControllerConfig = config.recorderControllerConfig.copy(
                                                    windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.map {
                                                        if (it == windowNamePattern) {
                                                            it.copy(windowName = value)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                )
                                            )
                                        },
                                        label = { Text("Window Name") },
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Button(
                                        onClick = {
                                            config = config.copy(
                                                recorderControllerConfig = config.recorderControllerConfig.copy(
                                                    windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.filterNot {
                                                        it == windowNamePattern
                                                    }
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                                    ) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }

                    var sleepIntervalIsError by remember { mutableStateOf(false) }

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
                        label = { Text("Sleep Interval [sec])") }
                    )

                    var maxRecordingDurationIsError by remember { mutableStateOf(false) }

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
                            Text(
                                "Max recording duration [min]\n(MP3 file size limit is 25MB..\nIf OpenAPI returns " +
                                        "an error, you need to decrease this value.)"
                            )
                        }
                    )

                    Box(modifier = Modifier.padding(top = 10.dp)) {
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
        }
    }
}
