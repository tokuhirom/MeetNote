package meetnote

import org.slf4j.LoggerFactory

object NotificationSender {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendMessage(message: String) {
        try {
            val processBuilder = ProcessBuilder("osascript", "-e",
                """display notification "$message" with title "MeetNote""""
            )

            val process = processBuilder.start()
            process.waitFor()
        } catch (e: Exception) {
            logger.info("Error in sending notification: $e", e)
        }
    }
}
