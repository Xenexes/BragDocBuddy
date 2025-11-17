package api.cli.presenters

import domain.PullRequest
import domain.PullRequestSyncResult

class PullRequestSyncPresenter {
    companion object {
        private const val SEPARATOR_WIDTH = 80
    }

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
        println("=".repeat(SEPARATOR_WIDTH))
        pullRequests.forEach { pr ->
            println(pr.url)
        }
        println("=".repeat(SEPARATOR_WIDTH))
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
