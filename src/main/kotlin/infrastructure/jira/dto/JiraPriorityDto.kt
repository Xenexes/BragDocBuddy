package infrastructure.jira.dto

import kotlinx.serialization.Serializable

@Serializable
data class JiraPriorityDto(
    val name: String,
)
