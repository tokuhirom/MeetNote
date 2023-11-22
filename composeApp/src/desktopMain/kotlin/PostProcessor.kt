import com.aallam.openai.api.audio.AudioResponseFormat
import com.aallam.openai.api.audio.transcriptionRequest
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.chatCompletionRequest
import com.aallam.openai.api.chat.chatMessage
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PostProcessor(val openAI: OpenAI) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun process(wavePath: Path) {
        logger.info("Starting post-processing: $wavePath")

        val mp3Path = convertToMp3(wavePath)

        val txtFilePath = speechToText(mp3Path)

        summarize(txtFilePath)
    }

    private fun convertToMp3(wavePath: Path): Path {
        val mp3Path = wavePath.resolveSibling("${wavePath.fileName.toString().dropLast(4)}.mp3")

        try {
            val process = ProcessBuilder("lame", "-b", "64", "-m", "m", wavePath.toString(), mp3Path.toString()).start()
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

    private suspend fun summarize(txtFilePath: Path) {
        val mdPath = txtFilePath.resolveSibling("${txtFilePath.fileName.toString().dropLast(4)}.md")
        val resp = openAI.chatCompletion(
            chatCompletionRequest {
                model = ModelId("gpt-4-32k")
                messages = listOf(
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
            }
        )
        mdPath.writeText(resp.choices[0].message.content!!)
    }

    private suspend fun speechToText(mp3Path: Path): Path {
        val txtPath = mp3Path.resolveSibling("${mp3Path.fileName.toString().dropLast(4)}.txt")
        logger.info("Transcripting $mp3Path(${mp3Path.fileSize()} bytes) to $txtPath")

        val res = openAI.transcription(
            transcriptionRequest {
                audio = FileSource(mp3Path.toOkioPath(), FileSystem.SYSTEM)
                model = ModelId("whisper-1")
                responseFormat = AudioResponseFormat.Text
                language = "ja"
            }
        )
        logger.info("Writing result to $txtPath")
        txtPath.writeText(res.text)
        return txtPath
    }

}
