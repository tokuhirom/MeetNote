import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioSystem
import kotlin.io.path.name
import kotlin.io.path.readText

@Composable
fun App(dataRepository: DataRepository) {
    val logger = LoggerFactory.getLogger("App")

    MaterialTheme {
        var logs by remember { mutableStateOf(dataRepository.getRecentSummarizedFiles()) }

        val executor = Executors.newScheduledThreadPool(1)
        executor.scheduleAtFixedRate({
            logger.info("Loading recent summarized files")
            logs = dataRepository.getRecentSummarizedFiles()
         }, 0, 10, TimeUnit.SECONDS)

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(logs) {log ->
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(log.name)
                        // TODO delete button
                        // TODO edit button

                        Text(log.readText())
                    }

                    Divider()
                }
            }
        }
    }
}
