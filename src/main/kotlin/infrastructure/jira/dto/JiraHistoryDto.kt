package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraHistoryDto(
    val created: String,
    val items: List<JiraHistoryItemDto> = emptyList(),
)
