package api.cli

import domain.BragEntry
import domain.JiraIssue
import domain.JiraIssueSyncResult
import domain.PullRequest
import domain.PullRequestSyncResult
import domain.Timeframe
import infrastructure.version.VersionChecker
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import ports.UserInput
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import usecases.SyncJiraIssuesUseCase
import usecases.SyncPullRequestsUseCase
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

sealed interface Command {
    fun execute()

    class InitCommand(
        private val useCase: InitRepositoryUseCase,
    ) : Command {
        private val logger = KotlinLogging.logger {}

        override fun execute() {
            try {
                logger.info { "Initializing repository" }
                useCase.initRepository()
                println("Initialized bragging document directory")
                logger.info { "Repository initialized successfully" }
            } catch (e: IllegalStateException) {
                logger.error(e) { "Failed to initialize repository: ${e.message}" }
                System.err.println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }

    class AddCommand(
        private val useCase: AddBragUseCase,
        private val content: String,
    ) : Command {
        private val logger = KotlinLogging.logger {}

        override fun execute() {
            try {
                logger.info { "Adding brag entry" }
                useCase.addBragEntry(content)
                println("Added brag: $content")
                logger.info { "Brag entry added successfully" }
            } catch (e: IllegalStateException) {
                logger.error(e) { "Failed to add brag entry: ${e.message}" }
                System.err.println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }

    class ReviewCommand(
        private val useCase: GetBragsUseCase,
        private val timeframe: Timeframe,
        private val presenter: BragPresenter,
    ) : Command {
        private val logger = KotlinLogging.logger {}

        override fun execute() {
            try {
                logger.info { "Retrieving brags for timeframe: $timeframe" }
                val brags = useCase.getBrags(timeframe)
                logger.info { "Found ${brags.values.sumOf { it.size }} brag entries" }
                presenter.present(brags)
            } catch (e: IllegalStateException) {
                logger.error(e) { "Failed to retrieve brags: ${e.message}" }
                System.err.println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }

    class VersionCommand(
        private val versionChecker: VersionChecker,
    ) : Command {
        override fun execute() {
            versionChecker.checkForUpdates()
        }
    }

    class SyncPullRequestsCommand(
        private val useCase: SyncPullRequestsUseCase,
        private val timeframe: Timeframe,
        private val printOnly: Boolean,
        private val presenter: PullRequestSyncPresenter,
    ) : Command {
        private val logger = KotlinLogging.logger {}

        override fun execute() {
            runBlocking {
                try {
                    logger.info { "Executing sync pull requests command (timeframe: $timeframe, printOnly: $printOnly)" }
                    val result = useCase.syncPullRequests(timeframe, printOnly)
                    presenter.present(result)
                    logger.info { "Pull requests sync command completed successfully" }
                } catch (e: IllegalStateException) {
                    logger.error(e) { "Failed to sync pull requests: ${e.message}" }
                    System.err.println("Error: ${e.message}")
                    exitProcess(1)
                }
            }
        }
    }

    class SyncJiraIssuesCommand(
        private val useCase: SyncJiraIssuesUseCase,
        private val timeframe: Timeframe,
        private val printOnly: Boolean,
        private val presenter: JiraIssueSyncPresenter,
        private val userInput: UserInput,
    ) : Command {
        private val logger = KotlinLogging.logger {}

        override fun execute() {
            runBlocking {
                try {
                    logger.info { "Executing sync Jira issues command (timeframe: $timeframe, printOnly: $printOnly)" }
                    val result = useCase.syncJiraIssues(timeframe, printOnly)
                    when (result) {
                        is JiraIssueSyncResult.ReadyToSync -> {
                            logger.info { "Ready to sync ${result.issues.size} Jira issues, waiting for user selection" }
                            presenter.presentInteractive(result.issues, userInput) { selectedIssues ->
                                logger.info { "User selected ${selectedIssues.size} issues to sync" }
                                val syncResult = useCase.syncSelectedIssues(selectedIssues)
                                presenter.presentSyncResult(syncResult)
                            }
                        }
                        else -> presenter.present(result)
                    }
                    logger.info { "Jira issues sync command completed successfully" }
                } catch (e: IllegalStateException) {
                    logger.error(e) { "Failed to sync Jira issues: ${e.message}" }
                    System.err.println("Error: ${e.message}")
                    exitProcess(1)
                }
            }
        }
    }

    class BragPresenter {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        fun present(bragsByDate: Map<String, List<BragEntry>>) {
            if (bragsByDate.isEmpty()) {
                println("No brags found in this time period.")
                return
            }

            println()
            bragsByDate.forEach { (date, entries) ->
                println(date)
                entries.forEach { entry ->
                    val timeStr = entry.timestamp.format(timeFormatter)
                    println("  * $timeStr ${entry.content}")
                }
                println()
            }
        }
    }

    class PullRequestSyncPresenter {
        fun present(result: PullRequestSyncResult) {
            when (result) {
                is PullRequestSyncResult.Disabled -> {
                    println("GitHub PR sync is disabled")
                }
                is PullRequestSyncResult.NotConfigured -> {
                    println(
                        """
                        GitHub PR sync is enabled but not configured.

                        Required environment variables:
                          BRAG_DOC_GITHUB_TOKEN (or use 'gh auth login')
                          BRAG_DOC_GITHUB_USERNAME
                          BRAG_DOC_GITHUB_ORG

                        To disable this feature, set:
                          BRAG_DOC_GITHUB_PR_SYNC_ENABLED=false
                        """.trimIndent(),
                    )
                }
                is PullRequestSyncResult.PrintOnly -> {
                    presentPullRequestList(result.pullRequests)
                }
                is PullRequestSyncResult.Synced -> {
                    presentSyncResult(result.addedCount, result.skippedCount, "pull request")
                }
            }
        }

        private fun presentPullRequestList(pullRequests: List<PullRequest>) {
            if (pullRequests.isEmpty()) {
                println("No merged pull requests found")
                return
            }

            println()
            println("Merged Pull Requests:")
            println("=".repeat(80))
            pullRequests.forEach { pr ->
                println(pr.url)
            }
            println("=".repeat(80))
            println()
            println("Total: ${pullRequests.size} merged pull requests")
        }

        private fun presentSyncResult(
            addedCount: Int,
            skippedCount: Int,
            itemType: String,
        ) {
            if (addedCount == 0 && skippedCount == 0) {
                println("No merged pull requests found to add to brag document")
                return
            }

            when {
                skippedCount == 0 -> {
                    println("Successfully added $addedCount merged $itemType${if (addedCount != 1) "s" else ""} to brag document")
                }
                addedCount == 0 -> {
                    println(
                        "All $skippedCount $itemType${if (skippedCount != 1) "s were" else " was"} already in brag document (skipped duplicates)",
                    )
                }
                else -> {
                    println(
                        "Successfully added $addedCount merged $itemType${if (addedCount != 1) "s" else ""} to brag document ($skippedCount duplicate${if (skippedCount != 1) "s" else ""} skipped)",
                    )
                }
            }
        }
    }

    class JiraIssueSyncPresenter {
        fun present(result: JiraIssueSyncResult) {
            when (result) {
                is JiraIssueSyncResult.Disabled -> {
                    println("Jira issue sync is disabled")
                }
                is JiraIssueSyncResult.NotConfigured -> {
                    println(
                        """
                        Jira issue sync is enabled but not configured.

                        Required environment variables:
                          BRAG_DOC_JIRA_URL (e.g., https://your-company.atlassian.net)
                          BRAG_DOC_JIRA_EMAIL
                          BRAG_DOC_JIRA_API_TOKEN

                        To disable this feature, set:
                          BRAG_DOC_JIRA_SYNC_ENABLED=false
                        """.trimIndent(),
                    )
                }
                is JiraIssueSyncResult.PrintOnly -> {
                    presentIssueList(result.issues, urlOnly = true)
                }
                is JiraIssueSyncResult.Synced -> {
                    presentSyncResult(result)
                }
                is JiraIssueSyncResult.ReadyToSync -> {
                    // This case is handled by presentInteractive
                }
            }
        }

        fun presentInteractive(
            issues: List<JiraIssue>,
            userInput: UserInput,
            onConfirm: (List<JiraIssue>) -> Unit,
        ) {
            if (issues.isEmpty()) {
                println("No resolved Jira issues found to add to brag document")
                return
            }

            presentIssueList(issues, urlOnly = false)

            println("Enter issue keys to skip (comma-separated), or press Enter to add all:")
            val input = userInput.readLine("> ")?.trim() ?: ""

            val skipKeys =
                if (input.isBlank()) {
                    emptySet()
                } else {
                    input.split(",").map { it.trim().uppercase() }.toSet()
                }

            val selectedIssues =
                if (skipKeys.isEmpty()) {
                    issues
                } else {
                    issues.filter { !skipKeys.contains(it.key) }
                }

            if (selectedIssues.isEmpty()) {
                println("All issues skipped. No issues added to brag document.")
                return
            }

            onConfirm(selectedIssues)
        }

        fun presentSyncResult(result: JiraIssueSyncResult.Synced) {
            val addedCount = result.addedCount
            val skippedCount = result.skippedCount

            println()
            when {
                skippedCount == 0 -> {
                    println("Successfully added $addedCount Jira issue${if (addedCount != 1) "s" else ""} to brag document")
                }
                addedCount == 0 -> {
                    println(
                        "All $skippedCount issue${if (skippedCount != 1) "s were" else " was"} already in brag document (skipped duplicates)",
                    )
                }
                else -> {
                    println(
                        "Successfully added $addedCount Jira issue${if (addedCount != 1) "s" else ""} to brag document ($skippedCount duplicate${if (skippedCount != 1) "s" else ""} skipped)",
                    )
                }
            }
        }

        private fun presentIssueList(
            issues: List<JiraIssue>,
            urlOnly: Boolean,
        ) {
            println()
            println("Resolved Jira Issues:")
            println("=".repeat(80))
            issues.forEach { issue ->
                if (urlOnly) {
                    println(issue.url)
                } else {
                    println("[${issue.key}] ${issue.title}")
                    println("  ${issue.url}")
                }
            }
            println("=".repeat(80))
            println()
            println("Total: ${issues.size} resolved Jira issues")
            println()
        }
    }
}
