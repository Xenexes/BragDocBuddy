package domain.ai

import domain.ai.dto.TemplateType
import domain.ai.dto.ValidationResult

data class PromptTemplate(
    val type: TemplateType,
    val content: String,
) {
    fun validate(): ValidationResult {
        if (content.isBlank()) {
            return ValidationResult.Invalid("Template content cannot be empty")
        }

        val requiredPlaceholders = listOf("{entries}")
        val missingPlaceholders =
            requiredPlaceholders.filter { placeholder ->
                !content.contains(placeholder)
            }

        return if (missingPlaceholders.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                "Template missing required placeholders: ${missingPlaceholders.joinToString(", ")}",
            )
        }
    }

    fun fillPlaceholders(placeholders: Map<String, String>): String {
        var result = content
        placeholders.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}
