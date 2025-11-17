package infrastructure.jira.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JiraSearchJqlResponseDto(
    val issues: List<JiraIssueDto>,
    @SerialName("nextPageToken") val nextPageToken: String? = null,
    val total: Int? = null,
)
