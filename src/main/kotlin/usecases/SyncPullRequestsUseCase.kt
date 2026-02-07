package usecases

import domain.BragEntry
import domain.PullRequestSyncResult
import domain.TimeframeSpec
import domain.config.GitHubConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.BragRepository
import ports.GitHubClient
import ports.TimeframeParser

private val logger = KotlinLogging.logger {}

class SyncPullRequestsUseCase(
    private val gitHubClient: GitHubClient,
    private val bragRepository: BragRepository,
    private val timeframeParser: TimeframeParser,
    private val gitHubConfig: GitHubConfiguration,
) {
    suspend fun syncPullRequests(
        timeframeSpec: TimeframeSpec,
        printOnly: Boolean,
    ): PullRequestSyncResult {
        if (!gitHubConfig.enabled) {
            logger.info { "GitHub PR sync is disabled" }
            return PullRequestSyncResult.Disabled
        }

        if (!gitHubConfig.isConfigured()) {
            logger.warn { "GitHub PR sync is enabled but not configured" }
            return PullRequestSyncResult.NotConfigured
        }

        logger.info { "Syncing pull requests for timeframe: $timeframeSpec, printOnly: $printOnly" }

        val dateRange = timeframeParser.parse(timeframeSpec)
        val pullRequests =
            gitHubClient.fetchMergedPullRequests(
                organization = gitHubConfig.organization!!,
                author = gitHubConfig.username!!,
                dateRange = dateRange,
            )

        logger.info { "Found ${pullRequests.size} merged pull requests" }

        return if (printOnly) {
            PullRequestSyncResult.PrintOnly(pullRequests)
        } else {
            enrichBragDocument(pullRequests)
        }
    }

    private fun enrichBragDocument(pullRequests: List<domain.PullRequest>): PullRequestSyncResult.Synced {
        var addedCount = 0
        var skippedCount = 0

        pullRequests.forEach { pr ->
            val content =
                buildString {
                    append("[PR #${pr.number}] ${pr.title} - ${pr.url}")
                    if (!pr.description.isNullOrBlank()) {
                        append(" | ${pr.description}")
                    }
                }
            val entry =
                BragEntry(
                    timestamp = pr.mergedAt,
                    content = content,
                )
            val wasSaved = bragRepository.save(entry)
            if (wasSaved) {
                addedCount++
            } else {
                skippedCount++
            }
        }

        logger.info { "Added $addedCount pull requests, skipped $skippedCount duplicates" }
        return PullRequestSyncResult.Synced(addedCount, skippedCount)
    }
}
