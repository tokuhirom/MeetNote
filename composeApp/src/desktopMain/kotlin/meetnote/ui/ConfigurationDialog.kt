package meetnote.ui
import androidx.compose.foundation.border
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
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import meetnote.config.ConfigRepository
import meetnote.recordercontroller.WindowPattern
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.swing.JOptionPane

@Composable
fun configurationDialog(configRepository: ConfigRepository, onClose: () -> Unit) {
    val logger = LoggerFactory.getLogger("configurationDialog")

    DialogWindow(
        onCloseRequest = onClose,
        title = "Configuration",
        resizable = true,
        state = rememberDialogState(width = 800.dp, height = 600.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            var config by remember { mutableStateOf(configRepository.loadSettings()) }

            Box(modifier = Modifier.padding(bottom=10.dp).border(1.dp, Color.Gray)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("OpenAI Configuration", fontSize = TextUnit(1.5f, TextUnitType.Em))
                    TextField(
                        value = config.apiToken ?: "",
                        onValueChange = { config = config.copy(apiToken = it) },
                        label = { Text("API Token") }
                    )
                }
            }

            Box(modifier = Modifier.padding(bottom = 10.dp).border(1.dp, Color.Gray)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Recording Configuration", fontSize = TextUnit(1.5f, TextUnitType.Em))

                    Text("""
                        Please note: OpenAI's API does not support audio files larger than 25MB.
                        If you see an error related to this, please lower the max recording duration or bit rate.
                        """.trimIndent(), color = Color.Gray)

                    Row {
                        var maxRecordingDurationIsError by remember { mutableStateOf(false) }
                        TextField(
                            value = config.maxRecordingDuration.toMinutes().toString(),
                            onValueChange = { value ->
                                logger.info("Updating maxRecordingDuration: $value")
                                val longValue = value.toLongOrNull()
                                if (longValue != null) {
                                    config = config.copy(
                                        maxRecordingDuration = Duration.ofMinutes(longValue)
                                    )
                                    maxRecordingDurationIsError = false
                                } else {
                                    maxRecordingDurationIsError = true
                                }
                            },
                            isError = maxRecordingDurationIsError,
                            label = {
                                Text("Max recording duration [min]")
                            }
                        )

                        Spacer(Modifier.width(4.dp))

                        var mp3bitRateIsError by remember { mutableStateOf(false) }
                        TextField(
                            value = config.mp3bitRate.toString(),
                            onValueChange = { value ->
                                logger.info("Updating mp3bitRate: $value")
                                val intValue = value.toIntOrNull()
                                if (intValue != null) {
                                    config = config.copy(
                                        mp3bitRate = intValue
                                    )
                                    mp3bitRateIsError = false
                                } else {
                                    mp3bitRateIsError = true
                                }
                            },
                            isError = mp3bitRateIsError,
                            label = {
                                Text("MP3 bit rate")
                            }
                        )
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = 10.dp).border(1.dp, Color.Gray)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Window watcher configuration", fontSize = TextUnit(1.5f, TextUnitType.Em))

                    var sleepIntervalIsError by remember { mutableStateOf(false) }

                    TextField(
                        value = config.windowWatchConfig.watchInterval.toSeconds().toString(),
                        onValueChange = { value ->
                            logger.info("Updating sleep Interval: $value")
                            val longValue = value.toLongOrNull()
                            if (longValue != null) {
                                config = config.copy(
                                    windowWatchConfig = config.windowWatchConfig.copy(
                                        watchInterval = Duration.ofSeconds(longValue)
                                    )
                                )
                                sleepIntervalIsError = false
                            } else {
                                sleepIntervalIsError = true
                            }
                        },
                        isError = sleepIntervalIsError,
                        label = { Text("Window watch Interval [sec])") }
                    )

                    Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))

                    Row {
                        Text("Window watching rule")

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                config = config.copy(
                                    windowWatchConfig = config.windowWatchConfig.copy(
                                        windowPatterns = config.windowWatchConfig.windowPatterns +
                                                WindowPattern("com.example", "Great new window")
                                    )
                                )
                            },
                        ) {
                            Text("Add")
                        }
                    }

                    Box(modifier = Modifier.padding(top = 10.dp)) {
                        LazyColumn {
                            items(config.windowWatchConfig.windowPatterns) { windowNamePattern ->
                                Row {
                                    TextField(
                                        value = windowNamePattern.bundleId,
                                        onValueChange = {value ->
                                            config = config.copy(
                                                windowWatchConfig = config.windowWatchConfig.copy(
                                                    windowPatterns = config.windowWatchConfig.windowPatterns.map {
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
                                                windowWatchConfig = config.windowWatchConfig.copy(
                                                    windowPatterns = config.windowWatchConfig.windowPatterns.map {
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
                                                windowWatchConfig = config.windowWatchConfig.copy(
                                                    windowPatterns = config.windowWatchConfig.windowPatterns.filterNot {
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
                }
            }


            Box(modifier = Modifier.padding(top = 10.dp)) {
                Button(
                    onClick = {
                        configRepository.saveConfiguration(config)

                        JOptionPane.showMessageDialog(
                            null,
                            "Configuration have been updated. Reboot the application, please.",
                            "Configuration Update",
                            JOptionPane.INFORMATION_MESSAGE
                        )
                        onClose()
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
