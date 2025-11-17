package infrastructure.jira

import domain.JiraIssue
import infrastructure.jira.dto.JiraIssueDto
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object JiraIssueDomainMapper {
    private val jiraDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    fun toDomain(
        issue: JiraIssueDto,
        jiraUrl: String,
    ): JiraIssue {
        val transitionDate = findDoneTransitionDate(issue)

        val dateString =
            transitionDate
                ?: issue.fields.resolutiondate
                ?: issue.fields.updated

        val resolvedAtInstant = OffsetDateTime.parse(dateString, jiraDateTimeFormatter).toInstant()
        val resolvedAtDateTime = LocalDateTime.ofInstant(resolvedAtInstant, ZoneOffset.UTC)

        return JiraIssue(
            key = issue.key,
            title = issue.fields.summary,
            url = "$jiraUrl/browse/${issue.key}",
            resolvedAt = resolvedAtDateTime,
        )
    }

    private fun findDoneTransitionDate(issue: JiraIssueDto): String? {
        // Find the most recent transition where status changed
        // Look for transitions to the current status (which is in "Done" category based on JQL)
        val currentStatus = issue.fields.status.name

        return issue.changelog
            ?.histories
            ?.asReversed()
            ?.firstOrNull { history ->
                history.items.any { item ->
                    item.field == "status" && item.toString == currentStatus
                }
            }?.created
    }
}
