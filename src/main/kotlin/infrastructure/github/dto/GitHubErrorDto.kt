package infrastructure.github.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubErrorDto(
    val message: String,
    @SerialName("documentation_url") val documentationUrl: String? = null,
)
