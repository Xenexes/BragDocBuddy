package api.cli

import domain.BragEntry
import domain.PullRequest
import domain.PullRequestSyncResult
import domain.Timeframe
import infrastructure.version.VersionChecker
import kotlinx.coroutines.runBlocking
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import usecases.SyncPullRequestsUseCase
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

sealed interface Command {
    fun execute()

    class InitCommand(
        private val useCase: InitRepositoryUseCase,
    ) : Command {
        override fun execute() {
            try {
                useCase.initRepository()
                println("Initialized bragging document directory")
            } catch (e: IllegalStateException) {
                println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }

    class AddCommand(
        private val useCase: AddBragUseCase,
        private val content: String,
    ) : Command {
        override fun execute() {
            try {
                useCase.addBragEntry(content)
                println("Added brag: $content")
            } catch (e: IllegalStateException) {
                println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }

    class ReviewCommand(
        private val useCase: GetBragsUseCase,
        private val timeframe: Timeframe,
        private val presenter: BragPresenter,
    ) : Command {
        override fun execute() {
            try {
                val brags = useCase.getBrags(timeframe)
                presenter.present(brags)
            } catch (e: IllegalStateException) {
                println("Error: ${e.message}")
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
        override fun execute() {
            runBlocking {
                try {
                    val result = useCase.syncPullRequests(timeframe, printOnly)
                    presenter.present(result)
                } catch (e: IllegalStateException) {
                    println("Error: ${e.message}")
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
}
