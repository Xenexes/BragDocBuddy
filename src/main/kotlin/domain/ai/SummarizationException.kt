package domain.ai

class SummarizationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
