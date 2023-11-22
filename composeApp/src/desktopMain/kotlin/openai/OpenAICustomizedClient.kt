package openai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.util.*
import java.io.File


data class TranscriptionResponse(val text: String)

// transcriptions が java.io.IOException: Stream Closed になるので、直接 ktor で動かす。
class OpenAICustomizedClient(private val openAIApiKey: String) {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 1000 * 60 * 10
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
            println("Transcribed text: $data")
            return data
        } else {
            throw RuntimeException("Transcription failed. Status: ${response.status}")
        }
    }
}
