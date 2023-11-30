package meetnote

import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.deleteIfExists

fun deleteFileWithSameNameVtt(file: File) {
    val vttFilePath = file.absolutePath.replace(".md", ".vtt")
    val vttFile = Paths.get(vttFilePath)
    vttFile.deleteIfExists()
    file.delete()
}

fun startFileWatcher(path: Path, callback: () -> Unit) {
    val logger = LoggerFactory.getLogger("startFileWatcher")
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
