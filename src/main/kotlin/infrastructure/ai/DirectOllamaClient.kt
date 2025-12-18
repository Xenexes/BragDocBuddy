package infrastructure.ai

import domain.config.OllamaConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ports.AIClient

private val logger = KotlinLogging.logger {}

/**
 * Direct Ollama HTTP client without Koog framework.
 *
 * This implementation bypasses the Koog framework serialization issues
 * and communicates directly with Ollama's REST API.
 *
 * **Why This Exists**:
 * Koog 0.5.3 has a serialization bug (Issue #190) with OllamaChatResponseDTO
 * that causes crashes when Ollama returns responses. This direct client
 * works around that issue by using our own DTO classes.
 *
 * @see https://github.com/JetBrains/koog/issues/190
 */
class DirectOllamaClient(
    private val configuration: OllamaConfiguration,
) : AIClient,
    AutoCloseable {
    private val httpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                    },
                )
            }
        }

    /**
     * Generate a response from Ollama using the /api/generate endpoint.
     *
     * @param userPrompt The prompt to send to the LLM
     * @param systemPrompt Optional system prompt
     * @param model Optional model override
     * @return The generated response text
     */
    override suspend fun generate(
        userPrompt: String,
        systemPrompt: String?,
        model: String?,
    ): String {
        if (!configuration.isConfigured()) {
            throw IllegalStateException(
                "Ollama is not configured. " +
                    "Please set BRAG_DOC_OLLAMA_ENABLED=true, BRAG_DOC_OLLAMA_URL, and BRAG_DOC_OLLAMA_MODEL",
            )
        }

        val modelToUse = model ?: configuration.model
        logger.info { "Generating response using direct Ollama client with model: $modelToUse" }

        val finalPrompt =
            if (systemPrompt != null) {
                "$systemPrompt\n\n$userPrompt"
            } else {
                userPrompt
            }

        val request =
            OllamaGenerateRequest(
                model = modelToUse,
                prompt = finalPrompt,
                stream = false,
            )

        return try {
            // Ollama returns NDJSON (newline-delimited JSON) with streaming enabled
            // When stream=false, it returns multiple lines with partial responses
            // We need to accumulate the response field from all lines
            val responseText =
                httpClient
                    .post("${configuration.url}/api/generate") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }.bodyAsText()

            val lines = responseText.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                throw IllegalStateException("Empty response from Ollama")
            }

            val fullResponse = StringBuilder()
            for (line in lines) {
                try {
                    val partialResponse = Json.decodeFromString<OllamaGenerateResponse>(line)
                    fullResponse.append(partialResponse.response)
                } catch (e: Exception) {
                    logger.warn { "Skipping malformed line: $line" }
                }
            }

            val result = fullResponse.toString()
            logger.debug { "Ollama response received (${result.length} chars)" }
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to call Ollama directly" }
            throw IllegalStateException(
                "Failed to generate response from Ollama: ${e.message}. " +
                    "Make sure Ollama is running at ${configuration.url} and model '$modelToUse' is available.",
                e,
            )
        }
    }

    override fun close() {
        logger.debug { "Closing direct Ollama HTTP client" }
        httpClient.close()
    }
}

@Serializable
data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val system: String? = null,
)

@Serializable
data class OllamaGenerateResponse(
    val model: String,
    @SerialName("created_at") val createdAt: String,
    val response: String,
    val done: Boolean,
    @SerialName("done_reason") val doneReason: String? = null,
    val context: List<Int>? = null,
    @SerialName("total_duration") val totalDuration: Long? = null,
    @SerialName("load_duration") val loadDuration: Long? = null,
    @SerialName("prompt_eval_count") val promptEvalCount: Int? = null,
    @SerialName("prompt_eval_duration") val promptEvalDuration: Long? = null,
    @SerialName("eval_count") val evalCount: Int? = null,
    @SerialName("eval_duration") val evalDuration: Long? = null,
)
