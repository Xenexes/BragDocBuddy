package infrastructure.github.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PullRequestRefDto(
    @SerialName("merged_at") val mergedAt: String? = null,
)
