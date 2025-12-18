package infrastructure.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import domain.config.OllamaConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging

class SimpleKoogAIService(
    private val configuration: OllamaConfiguration,
) : AutoCloseable {
    private val logger = KotlinLogging.logger {}

    suspend fun generate(
        userPrompt: String,
        systemPrompt: String? = null,
        model: String? = null,
    ): String {
        if (!configuration.isConfigured()) {
            throw IllegalStateException(
                "Ollama is not configured. " +
                    "Please set BRAG_DOC_OLLAMA_ENABLED=true, BRAG_DOC_OLLAMA_URL, and BRAG_DOC_OLLAMA_MODEL",
            )
        }

        val modelToUse = parseOllamaModel(model ?: configuration.model)
        logger.info { "Generating response using Koog with Ollama model: ${modelToUse.id}" }

        return try {
            val agent =
                AIAgent(
                    promptExecutor =
                        simpleOllamaAIExecutor(
                            baseUrl = configuration.url,
                        ),
                    llmModel = modelToUse,
                    systemPrompt = systemPrompt ?: "You are a helpful AI assistant.",
                )

            val response = agent.run(userPrompt)

            logger.debug { "Ollama response received (${response.length} chars)" }
            response
        } catch (e: Exception) {
            logger.error(e) { "Failed to call Ollama via Koog" }
            throw IllegalStateException(
                "Failed to generate response from Ollama: ${e.message}. " +
                    "Make sure Ollama is running at ${configuration.url} and model '${modelToUse.id}' is available.",
                e,
            )
        }
    }

    suspend fun generateWithSystemPrompt(
        systemPrompt: String,
        userPrompt: String,
        model: String? = null,
    ): String = generate(userPrompt = userPrompt, systemPrompt = systemPrompt, model = model)

    private fun parseOllamaModel(modelName: String): ai.koog.prompt.llm.LLModel =
        when (modelName.lowercase()) {
            "llama3.1", "llama-3.1" -> {
                // llama3.1 - create as custom model since not in OllamaModels constants
                logger.debug { "Using llama3.1 model" }
                ai.koog.prompt.llm.LLModel(
                    provider = ai.koog.prompt.llm.LLMProvider.Ollama,
                    id = "llama3.1:latest",
                    capabilities =
                        listOf(
                            ai.koog.prompt.llm.LLMCapability.Temperature,
                            ai.koog.prompt.llm.LLMCapability.Schema.JSON.Basic,
                            ai.koog.prompt.llm.LLMCapability.Tools,
                        ),
                    contextLength = 131_072,
                )
            }

            "llama3.2", "llama-3.2" -> OllamaModels.Meta.LLAMA_3_2
            "llama3.2:3b" -> OllamaModels.Meta.LLAMA_3_2_3B
            "llama4", "llama-4" -> OllamaModels.Meta.LLAMA_4
            "qwen2.5", "qwen-2.5" -> OllamaModels.Alibaba.QWEN_2_5_05B
            "qwen2.5-coder" -> OllamaModels.Alibaba.QWEN_CODER_2_5_32B
            "qwq" -> OllamaModels.Alibaba.QWQ
            "granite3.2-vision" -> OllamaModels.Granite.GRANITE_3_2_VISION
            else -> {
                logger.debug { "Using custom Ollama model: $modelName" }
                ai.koog.prompt.llm.LLModel(
                    provider = ai.koog.prompt.llm.LLMProvider.Ollama,
                    id = modelName,
                    capabilities =
                        listOf(
                            ai.koog.prompt.llm.LLMCapability.Temperature,
                            ai.koog.prompt.llm.LLMCapability.Schema.JSON.Basic,
                            ai.koog.prompt.llm.LLMCapability.Tools,
                        ),
                    contextLength = 8192,
                )
            }
        }

    override fun close() {
        logger.debug { "Closing Koog AI service" }
    }
}
