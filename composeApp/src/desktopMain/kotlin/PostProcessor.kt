import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.chat.chatMessage
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import openai.OpenAICustomizedClient
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PostProcessor(private val openAI: OpenAI, private val openAICustomizedClient: OpenAICustomizedClient) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun process(wavePath: Path) {
        logger.info("Starting post-processing: $wavePath")

        val mp3Path = convertToMp3(wavePath)

        val txtFilePath = speechToText(mp3Path)
        summarize(txtFilePath)

        cleanup(wavePath, mp3Path)
    }

    private fun cleanup(wavePath: Path, mp3Path: Path) {
        logger.info("Removing used files: $wavePath, $mp3Path")
        wavePath.toFile().delete()
        mp3Path.toFile().delete()
    }

    private fun convertToMp3(wavePath: Path): Path {
        val mp3Path = wavePath.resolveSibling("${wavePath.fileName.toString().dropLast(4)}.mp3")

        logger.info("Converting $wavePath(${wavePath.fileSize()} bytes) to mp3")

        try {
            val processBuilder = ProcessBuilder("lame", "--verbose", "-b", "64", "-m", "m", wavePath.toString(), mp3Path.toString())
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val reader = InputStreamReader(process.inputStream)
            reader.useLines {
                logger.info("[LAME $mp3Path] ${it.joinToString(" ")}")
            }

            process.waitFor()

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                logger.error("Error during mp3 conversion. Exit code: $exitCode.")
            } else {
                logger.info("Converted to mp3: $mp3Path(${mp3Path.fileSize()} bytes) from $wavePath(${wavePath.fileSize()} bytes)")
            }
        } catch (ex: Exception) {
            println("Failed to convert wave file. Error: ${ex.message}")
        }
        return mp3Path
    }


    private suspend fun speechToText(mp3Path: Path): Path {
        val txtPath = mp3Path.resolveSibling("${mp3Path.fileName.toString().dropLast(4)}.vtt")
        logger.info("Transcribing $mp3Path(${mp3Path.fileSize()} bytes) to $txtPath")

        val res = openAICustomizedClient.transcript(mp3Path.toFile(), "ja")
        logger.info("Writing result to $txtPath")

        txtPath.writeText(res)
        return txtPath
    }

    private suspend fun summarize(txtFilePath: Path) {
        val mdPath = txtFilePath.resolveSibling("${txtFilePath.fileName.toString().dropLast(4)}.md")
        val chatMessages = listOf(
            chatMessage {
                role = ChatRole.System
                content = """
                            Please summarize the main discussions and conclusions of this
                             meeting and organize the result in Markdown format. Specifically,
                              present the title as a section header on the first line, followed
                               by the content in bullet point format. The purpose is to make
                                the content easily comprehensible for later review.
                           Output text must be in Japanese.
                        """.trimIndent()
            },
            chatMessage {
                role = ChatRole.User
                content = txtFilePath.readText()
            }
        )
        val resp = openAI.chatCompletion(
            chatCompletionRequest {
                model = ModelId("gpt-4-32k")
                messages = chatMessages
            }
        )
        logger.info("Writing result to $mdPath")
        mdPath.writeText(resp.choices[0].message.content!!)
    }

}
