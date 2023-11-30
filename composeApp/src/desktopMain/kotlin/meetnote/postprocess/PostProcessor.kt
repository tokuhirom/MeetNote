package meetnote.postprocess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import meetnote.audio.rms
import meetnote.openai.ChatCompletionRequest
import meetnote.openai.Message
import meetnote.openai.OpenAICustomizedClient
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.file.Path
import javax.sound.sampled.AudioSystem
import kotlin.io.path.fileSize
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PostProcessor(
    private val openAICustomizedClient: OpenAICustomizedClient,
    private val mp3bitRate: Int
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    var state by mutableStateOf("")

    suspend fun process(wavePath: Path) {
        logger.info("Starting post-processing: $wavePath")
        state = "Starting post-processing: $wavePath"

        val rms = getRms(wavePath)
        logger.info("The files RMS is $rms: $wavePath")
        if (rms == 0.0) { // maybe < 0.02 is good enough.
            logger.info("This file does not contain sound. $wavePath")
            cleanup(wavePath)
            state = ""
            return
        }

        val mp3Path = convertToMp3(wavePath)

        val txtFilePath = speechToText(mp3Path)
        summarize(txtFilePath)

        cleanup(wavePath)
        state = ""
    }

    private fun getRms(wavePath: Path): Double {
        AudioSystem.getAudioInputStream(wavePath.toFile()).use {
            return it.rms()
        }
    }

    private fun cleanup(wavePath: Path) {
        logger.info("Removing used files: $wavePath")
        wavePath.toFile().delete()
    }

    private fun convertToMp3(wavePath: Path): Path {
        val mp3Path = wavePath.resolveSibling("${wavePath.fileName.toString().dropLast(4)}.mp3")

        state = "lame processing..."
        logger.info("Converting $wavePath(${wavePath.fileSize()} bytes) to mp3. bitrate: $mp3bitRate")

        try {
            // --abr: average bitrate
            val processBuilder = ProcessBuilder("lame", "--verbose", "-v", "--abr", mp3bitRate.toString(), "-m", "m", wavePath.toString(), mp3Path.toString())
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
            logger.error("Failed to convert wave file. Error: ${ex.message}", ex)
        }
        return mp3Path
    }


    private suspend fun speechToText(mp3Path: Path): Path {
        val txtPath = mp3Path.resolveSibling("${mp3Path.fileName.toString().dropLast(4)}.vtt")
        state = "Transcribing..."
        logger.info("Transcribing $mp3Path(${mp3Path.fileSize()} bytes) to $txtPath")

        val res = openAICustomizedClient.transcript(mp3Path.toFile(), "ja")
        logger.info("Writing result to $txtPath")

        txtPath.writeText(res)
        return txtPath
    }

    private suspend fun summarize(txtFilePath: Path) {
        state = "Summarizing..."

        logger.info("Summarizing $txtFilePath")

        val mdPath = txtFilePath.resolveSibling("${txtFilePath.fileName.toString().dropLast(4)}.md")
        val chatMessages = listOf(
            Message(
                role = "system",
                content = """
                            Please summarize the main discussions and conclusions of this
                             meeting and organize the result in Markdown format. Specifically,
                              present the title as a section header on the first line, followed
                               by the content in bullet point format. The purpose is to make
                                the content easily comprehensible for later review.
                           Output text must be in Japanese.
                        """.trimIndent()
            ),
            Message(
                role = "user",
                content = txtFilePath.readText()
            )
        )
        val resp = openAICustomizedClient.chatCompletion(
            ChatCompletionRequest(
                "gpt-4-32k",
                chatMessages
            )
        )
        logger.info("Writing result to $mdPath")
        mdPath.writeText(resp.choices[0].message.content)
    }

}
