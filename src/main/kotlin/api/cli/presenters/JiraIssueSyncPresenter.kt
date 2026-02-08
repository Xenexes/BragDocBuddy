package api.cli.presenters

import domain.JiraIssue
import domain.JiraIssueSyncResult
import ports.UserInput

class JiraIssueSyncPresenter {
    companion object {
        private const val SEPARATOR_WIDTH = 80
    }

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
        println("=".repeat(SEPARATOR_WIDTH))
        issues.forEach { issue ->
            if (urlOnly) {
                println(issue.url)
            } else {
                val metadata =
                    listOfNotNull(issue.issueType, issue.status)
                        .filter { it.isNotBlank() }
                val suffix = if (metadata.isNotEmpty()) " (${metadata.joinToString(", ")})" else ""
                println("[${issue.key}] ${issue.title}$suffix")
                println("  ${issue.url}")
            }
        }
        println("=".repeat(SEPARATOR_WIDTH))
        println()
        println("Total: ${issues.size} resolved Jira issues")
        println()
    }
}
