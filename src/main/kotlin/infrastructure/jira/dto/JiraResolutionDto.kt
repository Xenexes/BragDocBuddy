package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraResolutionDto(
    val name: String,
)
