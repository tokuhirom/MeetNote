package meetnote.openai

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory
import java.io.File

data class Message(val role: String, val content: String)
data class ChatCompletionRequest(val model: String, val messages: List<Message>)

data class ChatChoice(
    val index: Int,
    val message: Message,

    @JsonProperty("finish_reason")
    val finishReason: String
)

data class ChatCompletionUsage(

    @JsonProperty("prompt_tokens")
    val promptTokens: Int,

    @JsonProperty("completion_tokens")
    val completionTokens: Int,

    @JsonProperty("total_tokens")
    val totalTokens: Int
)

data class ChatCompletionResponse(
    val id: String,

    @JsonProperty("object")
    val `object`: String,

    val created: Long,
    val model: String,

    @JsonProperty("system_fingerprint")
    val systemFingerprint: String?,

    val choices: List<ChatChoice>,
    val usage: ChatCompletionUsage
)

// transcriptions が java.io.IOException: Stream Closed になるので、直接 ktor で動かす。
class OpenAICustomizedClient(private val openAIApiKey: String) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 1000 * 60 * 10
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            jackson()
        }
    }

    suspend fun transcript(file: File, language: String): String {
        val response = client.submitFormWithBinaryData(
            url = "https://api.openai.com/v1/audio/transcriptions",
            formData = formData {
                append("model", "whisper-1")
                append("language", language)
                append("response_format", "vtt")
                append("file", file.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "audio/mpeg")
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                })
            }
        ) {
            timeout {
                requestTimeoutMillis = 1000 * 60 * 10
            }
            header("Authorization", "Bearer $openAIApiKey")
        }

        if (response.status == HttpStatusCode.OK) {
            val data = response.bodyAsText()
            logger.debug("Transcribed text: $data")
            return data
        } else {
            throw RuntimeException("Transcription failed. Status: ${response.status}")
        }
    }

    suspend fun chatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $openAIApiKey")
            setBody(request)
        }

        if (response.status == HttpStatusCode.OK) {
            val data = response.body<ChatCompletionResponse>()
            logger.debug("Chat completion result: {}", data)
            return data
        } else {
            throw RuntimeException("Chat completion failed. Status: ${response.status}")
        }
    }
}
