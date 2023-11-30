package meetnote.config

import meetnote.recordercontroller.ProcessPattern
import meetnote.recordercontroller.WindowPattern
import java.time.Duration

enum class RecorderControllerType {
    PROCESS,
    WINDOW_NAME
}

data class Config(
    var apiToken: String? = null,
    var maxRecordingDuration: Duration = Duration.ofMinutes(30),
    var mp3bitRate: Int = 58, // [kbps]

    var mixer: String? = null,
    var maxMp3StorageCapacityMegabyte: Long = 750,

    var recorderControllerType: RecorderControllerType = RecorderControllerType.PROCESS,
    var highCpuUsageConfig: HighCpuUsageConfig = HighCpuUsageConfig(),
    var windowWatchConfig: WindowWatchConfig = WindowWatchConfig(),
)

data class HighCpuUsageConfig(
    var processPatterns: List<ProcessPattern> = listOf(
        ProcessPattern("zoom.us", 8.0),
        ProcessPattern("com.apple.WebKit.GPU", 1.0)
    ),
    var measureInterval: Int = 3
)

data class WindowWatchConfig(
    var windowPatterns: List<WindowPattern> = listOf(
        WindowPattern("us.zoom.xos", "Zoom Meeting")
    ),
    var watchInterval: Duration = Duration.ofSeconds(1),
)
