package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraHistoryItemDto(
    val field: String,
    val fromString: String? = null,
    val toString: String? = null,
    val from: String? = null,
    val to: String? = null,
    val fieldtype: String? = null,
)
