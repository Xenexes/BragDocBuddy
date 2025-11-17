package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraIssueDto(
    val id: String,
    val key: String,
    val self: String,
    val fields: JiraFieldsDto,
    val changelog: JiraChangelogDto? = null,
)
