package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraStatusDto(
    val name: String,
)
