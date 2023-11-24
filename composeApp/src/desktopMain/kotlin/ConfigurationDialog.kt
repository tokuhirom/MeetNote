
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import org.slf4j.LoggerFactory
import java.time.Duration

@Composable
fun configurationDialog(configRepository: ConfigRepository, onClose: () -> Unit) {
    val logger = LoggerFactory.getLogger("configurationDialog")

    DialogWindow(onClose,
        title = "Configuration",
        resizable = true,
        state = rememberDialogState(size = DpSize(600.dp, 400.dp))) {
        Column(modifier = Modifier.padding(8.dp)) {
            var config by remember { mutableStateOf(configRepository.loadSettings()) }

            Box {
                Column {
                    Text("OpenAI Configuration")

                    TextField(
                        value = config.apiToken ?: "",
                        onValueChange = {
                            config.apiToken = it
                        },
                        label = {
                            Text("API Token")
                        }
                    )
                }
            }

            Box {
                Column {
                    Row {
                        Text("Recorder Controller Configurations")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                config = config.copy(
                                    recorderControllerConfig = config.recorderControllerConfig.copy(
                                        windowNamePatterns = config.recorderControllerConfig.windowNamePatterns + WindowNamePattern("com.example", "Great new window")
                                    )
                                )
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    LazyColumn {
                        items(config.recorderControllerConfig.windowNamePatterns) { windowNamePattern ->
                            Row {
                                TextField(
                                    value = windowNamePattern.bundleId,
                                    onValueChange = {
                                        config = config.copy(
                                            recorderControllerConfig = config.recorderControllerConfig.copy(
                                                windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.map {
                                                    if (it == windowNamePattern) {
                                                        windowNamePattern.copy(bundleId = it.bundleId)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            )
                                        )
                                    },
                                    label = {
                                        Text("Bundle ID")
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                TextField(
                                    value = windowNamePattern.windowName,
                                    onValueChange = {
                                        config = config.copy(
                                            recorderControllerConfig = config.recorderControllerConfig.copy(
                                                windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.map {
                                                    if (it == windowNamePattern) {
                                                        windowNamePattern.copy(windowName = it.windowName)
                                                    } else {
                                                        it
                                                    }
                                                }
                                            )
                                        )
                                    },
                                    label = {
                                        Text("Window Name")
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = {
                                        config = config.copy(
                                            recorderControllerConfig = config.recorderControllerConfig.copy(
                                                windowNamePatterns = config.recorderControllerConfig.windowNamePatterns.filter {
                                                    it != windowNamePattern
                                                }
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Red
                                    ),
                    //                            modifier = Modifier.weight(1f)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }

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
                }
            }


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
