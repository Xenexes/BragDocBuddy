package domain

sealed interface PullRequestSyncResult {
    data object Disabled : PullRequestSyncResult

    data object NotConfigured : PullRequestSyncResult

    data class PrintOnly(
        val pullRequests: List<PullRequest>,
    ) : PullRequestSyncResult

    data class Synced(
        val addedCount: Int,
        val skippedCount: Int,
    ) : PullRequestSyncResult
}
