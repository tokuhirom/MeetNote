package meetnote.postprocess

import kotlinx.coroutines.runBlocking
import meetnote.DataRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class PostProcessingResumer(private val dataRepository: DataRepository, private val postProcessor: PostProcessor) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val executors = Executors.newSingleThreadExecutor()

    fun start() {
        executors.submit {
            val files = dataRepository.getUnprocessedRawFiles()
            logger.info("Resuming processing ${files.size} files")

            files.forEach { file ->
                logger.info("Resuming processing $file")
                runBlocking {
                    postProcessor.process(file)
                }
            }

            logger.info("Resuming processing completed.")
        }
    }
}
