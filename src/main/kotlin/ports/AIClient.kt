package ports

/**
 * Port for AI/LLM client operations.
 *
 * This interface abstracts the AI service provider, allowing different
 * implementations (Ollama, OpenAI, Claude, etc.) without changing use cases.
 *
 * Following hexagonal architecture, use cases depend on this port,
 * not on concrete infrastructure implementations.
 */
interface AIClient {
    /**
     * Generate a response from the AI model.
     *
     * @param userPrompt The main prompt/question to send to the AI
     * @param systemPrompt Optional system prompt to set context/behavior
     * @param model Optional model override (if null, uses default)
     * @return The generated response text
     * @throws IllegalStateException if AI service is not configured or fails
     */
    suspend fun generate(
        userPrompt: String,
        systemPrompt: String? = null,
        model: String? = null,
    ): String
}
