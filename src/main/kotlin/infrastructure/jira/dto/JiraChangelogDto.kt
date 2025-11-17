package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraChangelogDto(
    val histories: List<JiraHistoryDto> = emptyList(),
)
