package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraIssueTypeDto(
    val name: String,
)
