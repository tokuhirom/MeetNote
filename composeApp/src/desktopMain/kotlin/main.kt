import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aallam.openai.client.OpenAI
import openai.OpenAICustomizedClient

fun main() = application {
    val configRepository = ConfigRepository()
    val config = configRepository.loadSettings()
    val dataRepository = DataRepository()
    val openAICustomizedClient = OpenAICustomizedClient(config.apiToken!!)
    val postProcessor = PostProcessor(OpenAI(config.apiToken!!), openAICustomizedClient)
    val postProcessingResumer = PostProcessingResumer(dataRepository, postProcessor)
    postProcessingResumer.start()

    Recorder(postProcessor, dataRepository).start()

    Window(onCloseRequest = ::exitApplication, title = "MeetNote", state = rememberWindowState()) {
        App(dataRepository)
    }
}
