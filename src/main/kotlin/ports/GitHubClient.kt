package ports

import domain.DateRange
import domain.PullRequest

interface GitHubClient {
    suspend fun fetchMergedPullRequests(
        organization: String,
        author: String,
        dateRange: DateRange,
    ): List<PullRequest>
}
