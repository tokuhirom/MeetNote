import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.aallam.openai.client.OpenAI

fun main() = application {
    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val postProcessor = PostProcessor(OpenAI(config.apiToken!!))
    Recorder(postProcessor).start()

    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}
