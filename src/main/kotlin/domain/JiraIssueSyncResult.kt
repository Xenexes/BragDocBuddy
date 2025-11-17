package domain

sealed interface JiraIssueSyncResult {
    data object Disabled : JiraIssueSyncResult

    data object NotConfigured : JiraIssueSyncResult

    data class PrintOnly(
        val issues: List<JiraIssue>,
    ) : JiraIssueSyncResult

    data class ReadyToSync(
        val issues: List<JiraIssue>,
    ) : JiraIssueSyncResult

    data class Synced(
        val addedCount: Int,
        val skippedCount: Int,
    ) : JiraIssueSyncResult
}
