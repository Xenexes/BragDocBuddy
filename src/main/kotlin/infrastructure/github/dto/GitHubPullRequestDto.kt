package infrastructure.github.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubPullRequestDto(
    val id: Long,
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("closed_at") val closedAt: String? = null,
    @SerialName("pull_request") val pullRequest: PullRequestRefDto? = null,
    val user: GitHubUserDto,
    @SerialName("repository_url") val repositoryUrl: String,
    val draft: Boolean = false,
)
