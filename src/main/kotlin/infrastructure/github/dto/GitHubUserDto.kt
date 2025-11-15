package infrastructure.github.dto

import kotlinx.serialization.Serializable

@Serializable
data class GitHubUserDto(
    val login: String,
    val id: Long,
)
