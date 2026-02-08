package ports

import domain.DateRange
import domain.JiraIssue

interface JiraClient {
    suspend fun fetchResolvedIssues(
        email: String,
        dateRange: DateRange,
    ): List<JiraIssue>

    suspend fun fetchIssuesByKeys(keys: Set<String>): List<JiraIssue>
}
