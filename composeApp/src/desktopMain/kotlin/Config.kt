import java.time.Duration

data class Config(
    var apiToken: String? = null,
    var recorderControllerConfig: RecorderControllerConfig = RecorderControllerConfig(),
)

data class RecorderControllerConfig(
    var windowNamePatterns: List<WindowNamePattern> = listOf(
        WindowNamePattern("us.zoom.xos", "Zoom Meeting")
    ),
    var sleepInterval: Duration = Duration.ofSeconds(1),
    var maxRecordingDuration: Duration = Duration.ofMinutes(30),
)
