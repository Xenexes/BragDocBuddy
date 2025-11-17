package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraUserDto(
    val displayName: String,
    val emailAddress: String? = null,
    val accountId: String? = null,
)
