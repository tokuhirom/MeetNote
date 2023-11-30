package meetnote

import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds

class FileWatcher(val path: Path) {
    lateinit var callback: () -> Unit
    private val logger = LoggerFactory.getLogger(javaClass)

    fun start() {
        val watchService = FileSystems.getDefault().newWatchService()

        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        Thread {
            logger.info("Starting file watching thread...: $path")

            while (true) {
                val key = watchService.take()
                logger.info("Got watch event.")
                val events = key.pollEvents()
                for (event in events) {
                    logger.info("Event: context=${event.context()}")
                    callback()
                    break
                }
                key.reset()
            }
        }.start()
    }
}
