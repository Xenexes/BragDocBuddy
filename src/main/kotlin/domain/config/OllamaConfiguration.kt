package domain.config

data class OllamaConfiguration(
    val url: String,
    val model: String,
    val enabled: Boolean,
    val templatesDir: String?,
) {
    fun isConfigured(): Boolean = enabled && url.isNotBlank() && model.isNotBlank()

    companion object {
        const val DEFAULT_URL = "http://localhost:11434"
        const val DEFAULT_MODEL = "llama3.2"
        const val DEFAULT_ENABLED = true
    }
}
