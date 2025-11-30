package usecases

import domain.BragEntry
import domain.JiraIssue
import domain.JiraIssueSyncResult
import domain.Timeframe
import domain.config.JiraConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.BragRepository
import ports.JiraClient
import ports.TimeframeParser

private val logger = KotlinLogging.logger {}

class SyncJiraIssuesUseCase(
    private val jiraClient: JiraClient,
    private val bragRepository: BragRepository,
    private val timeframeParser: TimeframeParser,
    private val jiraConfig: JiraConfiguration,
) {
    suspend fun syncJiraIssues(
        timeframe: Timeframe,
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

        logger.info { "Syncing Jira issues for timeframe: $timeframe, printOnly: $printOnly" }

        val dateRange = timeframeParser.parse(timeframe)
        val jiraIssues =
            jiraClient.fetchResolvedIssues(
                email = jiraConfig.email!!,
                dateRange = dateRange,
            )

        logger.info { "Found ${jiraIssues.size} resolved Jira issues" }

        return if (printOnly) {
            JiraIssueSyncResult.PrintOnly(jiraIssues)
        } else {
            JiraIssueSyncResult.ReadyToSync(jiraIssues)
        }
    }

    fun syncSelectedIssues(issues: List<JiraIssue>): JiraIssueSyncResult.Synced {
        var addedCount = 0
        var skippedCount = 0

        issues.forEach { issue ->
            val content = "[${issue.key}] ${issue.title} - ${issue.url}"
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
}
