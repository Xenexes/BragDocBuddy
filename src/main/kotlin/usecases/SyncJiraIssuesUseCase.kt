package usecases

import domain.BragEntry
import domain.JiraIssue
import domain.JiraIssueSyncResult
import domain.JiraTicketKeyExtractor
import domain.TimeframeSpec
import domain.config.GitHubConfiguration
import domain.config.JiraConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.BragRepository
import ports.GitHubClient
import ports.JiraClient
import ports.TimeframeParser

private val logger = KotlinLogging.logger {}

class SyncJiraIssuesUseCase(
    private val jiraClient: JiraClient,
    private val bragRepository: BragRepository,
    private val timeframeParser: TimeframeParser,
    private val jiraConfig: JiraConfiguration,
    private val gitHubClient: GitHubClient? = null,
    private val gitHubConfig: GitHubConfiguration? = null,
) {
    suspend fun syncJiraIssues(
        timeframeSpec: TimeframeSpec,
        printOnly: Boolean,
    ): JiraIssueSyncResult {
        if (!jiraConfig.enabled) {
            logger.info { "Jira issue sync is disabled" }
            return JiraIssueSyncResult.Disabled
        }

        if (!jiraConfig.isConfigured()) {
            logger.warn { "Jira issue sync is enabled but not configured" }
            return JiraIssueSyncResult.NotConfigured
        }

        logger.info { "Syncing Jira issues for timeframe: $timeframeSpec, printOnly: $printOnly" }

        val dateRange = timeframeParser.parse(timeframeSpec)
        val jqlIssues =
            jiraClient.fetchResolvedIssues(
                email = jiraConfig.email!!,
                dateRange = dateRange,
            )

        logger.info { "Found ${jqlIssues.size} resolved Jira issues from JQL" }

        val prExtractedIssues = extractIssuesFromPullRequests(dateRange, jqlIssues)

        val allIssues =
            mergeIssues(jqlIssues, prExtractedIssues)
                .sortedBy { it.resolvedAt }

        logger.info { "Total Jira issues after merge: ${allIssues.size}" }

        return if (printOnly) {
            JiraIssueSyncResult.PrintOnly(allIssues)
        } else {
            JiraIssueSyncResult.ReadyToSync(allIssues)
        }
    }

    private suspend fun extractIssuesFromPullRequests(
        dateRange: domain.DateRange,
        jqlIssues: List<JiraIssue>,
    ): List<JiraIssue> {
        if (gitHubClient == null || gitHubConfig == null) return emptyList()
        if (!gitHubConfig.enabled || !gitHubConfig.isConfigured()) return emptyList()

        logger.info { "Extracting JIRA ticket keys from pull requests" }

        val pullRequests =
            gitHubClient.fetchMergedPullRequests(
                organization = gitHubConfig.organization!!,
                author = gitHubConfig.username!!,
                dateRange = dateRange,
            )

        val allKeys =
            pullRequests
                .flatMap { pr ->
                    JiraTicketKeyExtractor.extractKeys(pr.title, pr.description, pr.branchName)
                }.toSet()

        logger.info { "Extracted ${allKeys.size} unique JIRA keys from ${pullRequests.size} PRs" }

        val jqlKeys = jqlIssues.map { it.key }.toSet()
        val newKeys = allKeys - jqlKeys

        if (newKeys.isEmpty()) {
            logger.info { "No additional JIRA keys found from PRs" }
            return emptyList()
        }

        logger.info { "Fetching ${newKeys.size} additional JIRA issues: $newKeys" }

        return jiraClient.fetchIssuesByKeys(newKeys)
    }

    private fun mergeIssues(
        jqlIssues: List<JiraIssue>,
        prExtractedIssues: List<JiraIssue>,
    ): List<JiraIssue> {
        val issuesByKey = jqlIssues.associateBy { it.key }.toMutableMap()
        prExtractedIssues.forEach { issue ->
            issuesByKey.putIfAbsent(issue.key, issue)
        }
        return issuesByKey.values.toList()
    }

    fun syncSelectedIssues(issues: List<JiraIssue>): JiraIssueSyncResult.Synced {
        var addedCount = 0
        var skippedCount = 0

        issues.forEach { issue ->
            val content = formatBragEntry(issue)
            val entry =
                BragEntry(
                    timestamp = issue.resolvedAt,
                    content = content,
                )
            val wasSaved = bragRepository.save(entry)
            if (wasSaved) {
                addedCount++
            } else {
                skippedCount++
            }
        }

        logger.info { "Added $addedCount Jira issues, skipped $skippedCount duplicates" }
        return JiraIssueSyncResult.Synced(addedCount, skippedCount)
    }

    private fun formatBragEntry(issue: JiraIssue): String =
        buildString {
            append("[${issue.key}] ${issue.title} - ${issue.url}")
            val metadata =
                listOfNotNull(issue.issueType, issue.status)
                    .filter { it.isNotBlank() }
            if (metadata.isNotEmpty()) {
                append(" (${metadata.joinToString(", ")})")
            }
        }
}
