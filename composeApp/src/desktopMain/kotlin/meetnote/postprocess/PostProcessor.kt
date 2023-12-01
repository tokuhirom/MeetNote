package meetnote.postprocess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import meetnote.audio.audioRms
import meetnote.openai.ChatCompletionRequest
import meetnote.openai.Message
import meetnote.openai.OpenAICustomizedClient
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

class PostProcessor(
    private val openAICustomizedClient: OpenAICustomizedClient,
    private val mp3bitRate: Int,
    private val rawSampleRate: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    var state by mutableStateOf("")

    suspend fun process(rawPath: Path) {
        logger.info("Starting post-processing: $rawPath")
        state = "Starting post-processing: $rawPath"

        val rms = getRms(rawPath)
        logger.info("The files RMS is $rms: $rawPath")
        if (rms == 0.0) { // maybe < 0.02 is good enough.
            logger.info("This file does not contain sound. $rawPath")
            cleanup(rawPath)
            state = ""
            return
        }

        val mp3Path = convertToMp3(rawPath)

        val txtFilePath = speechToText(mp3Path)
        summarize(txtFilePath)

        cleanup(rawPath)
        state = ""
    }

    private fun getRms(rawPath: Path): Double {
        rawPath.inputStream().use {
            return audioRms(it.readAllBytes())
        }
    }

    private fun cleanup(rawPath: Path) {
        logger.info("Removing used files: $rawPath")
        rawPath.toFile().delete()
    }

    private fun convertToMp3(rawPath: Path): Path {
        val mp3Path = rawPath.resolveSibling("${rawPath.fileName.toString().dropLast(4)}.mp3")

        state = "lame processing..."
        logger.info("Converting $rawPath(${rawPath.fileSize()} bytes) to mp3. bitrate: $mp3bitRate")

        try {
            // --abr: average bitrate
            val processBuilder = ProcessBuilder("lame", "--verbose", "-r", "-s", rawSampleRate.toString(), "-v", "--abr", mp3bitRate.toString(), "-m", "m", rawPath.toString(), mp3Path.toString())
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
                logger.info("Converted to mp3: $mp3Path(${mp3Path.fileSize()} bytes) from $rawPath(${rawPath.fileSize()} bytes)")
            }
        } catch (ex: Exception) {
            logger.error("Failed to convert raw file. Error: ${ex.message}", ex)
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
                           If the content doesn't contain any meaningful discussion, just output `NO_CONTENT`.
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
