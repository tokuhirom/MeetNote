package meetnote.config

import meetnote.recordercontroller.WindowPattern
import java.time.Duration

data class Config(
    var apiToken: String? = null,
    var maxRecordingDuration: Duration = Duration.ofMinutes(30),
    var mp3bitRate: Int = 58, // [kbps]

    var mixer: String? = null,

    var windowWatchConfig: WindowWatchConfig = WindowWatchConfig(),
)

data class WindowWatchConfig(
    var windowPatterns: List<WindowPattern> = listOf(
        WindowPattern("us.zoom.xos", "Zoom Meeting")
    ),
    var watchInterval: Duration = Duration.ofSeconds(1),
)
