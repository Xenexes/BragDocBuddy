package domain.ai.dto

sealed class ValidationResult {
    data object Valid : ValidationResult()

    data class Invalid(
        val reason: String,
    ) : ValidationResult()

    fun isValid(): Boolean = this is Valid

    fun errorMessage(): String? =
        when (this) {
            is Valid -> null
            is Invalid -> reason
        }
}
