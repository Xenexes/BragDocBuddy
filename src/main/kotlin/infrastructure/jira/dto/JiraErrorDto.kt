package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraErrorDto(
    val errorMessages: List<String>? = null,
    val errors: Map<String, String>? = null,
)
