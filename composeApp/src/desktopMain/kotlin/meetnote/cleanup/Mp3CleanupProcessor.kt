package meetnote.cleanup

import meetnote.DataRepository
import meetnote.config.Config
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize

// 古い mp3 ファイルを消す
class Mp3CleanupProcessor {
    val logger = LoggerFactory.getLogger(javaClass)

    fun run(dataRepository: DataRepository, config: Config) {
        // 起動時に一回だけ動かす。

        Thread {
            val logs = dataRepository.getRecentSummarizedLogs().filter {
                it.mp3Path.exists()
            }.sortedByDescending {
                it.path.absolutePathString()
            }

            var remains = config.maxMp3StorageCapacityMegabyte * 1024 * 1024
            val queue = LinkedList(logs)
            while (queue.isNotEmpty()) {
                val item = queue.poll()
                remains -= item.mp3Path.fileSize()
                if (remains < 0) {
                    logger.info("Delete file: ${item.mp3Path}")
                    val deleteIfExists = item.mp3Path.deleteIfExists()
                    if (deleteIfExists) {
                        logger.info("Deleted ${item.mp3Path}")
                    }
                }
            }
            logger.info("Remaining capacity is: $remains")
        }.start()
    }
}
