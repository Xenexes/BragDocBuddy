package infrastructure.jira.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JiraFieldsDto(
    val summary: String,
    val status: JiraStatusDto,
    val issuetype: JiraIssueTypeDto,
    val created: String,
    val updated: String,
    val assignee: JiraUserDto? = null,
    val reporter: JiraUserDto? = null,
    val priority: JiraPriorityDto? = null,
    val resolution: JiraResolutionDto? = null,
    val resolutiondate: String? = null,
    @SerialName("Engineer[User Picker (single user)]")
    val engineer: JiraUserDto? = null,
)
