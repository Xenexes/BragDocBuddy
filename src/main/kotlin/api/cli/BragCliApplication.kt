package api.cli

import api.cli.commands.AddCommand
import api.cli.commands.Command
import api.cli.commands.InitCommand
import api.cli.commands.ReviewCommand
import api.cli.commands.SyncJiraIssuesCommand
import api.cli.commands.SyncPullRequestsCommand
import api.cli.commands.VersionCommand
import api.cli.presenters.BragPresenter
import api.cli.presenters.JiraIssueSyncPresenter
import api.cli.presenters.PullRequestSyncPresenter
import domain.Timeframe
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ports.UserInput
import ports.VersionChecker
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import usecases.SyncJiraIssuesUseCase
import usecases.SyncPullRequestsUseCase
import kotlin.system.exitProcess

class BragCliApplication : KoinComponent {
    private val initRepositoryUseCase: InitRepositoryUseCase by inject()
    private val addBragUseCase: AddBragUseCase by inject()
    private val getBragsUseCase: GetBragsUseCase by inject()
    private val syncPullRequestsUseCase: SyncPullRequestsUseCase by inject()
    private val syncJiraIssuesUseCase: SyncJiraIssuesUseCase by inject()
    private val versionChecker: VersionChecker by inject()
    private val userInput: UserInput by inject()
    private val bragPresenter = BragPresenter()
    private val prSyncPresenter = PullRequestSyncPresenter()
    private val jiraSyncPresenter = JiraIssueSyncPresenter()

    fun run(args: Array<String>) {
        val command = parseCommand(args)
        command.execute()
    }

    private fun parseCommand(args: Array<String>): Command =
        when {
            args.isEmpty() -> {
                printUsage()
            }

            args[0] == "init" -> {
                InitCommand(initRepositoryUseCase)
            }

            args[0] == "version" -> {
                VersionCommand(versionChecker)
            }

            args[0] == "about" && args.size > 1 -> {
                val timeframe =
                    Timeframe.fromString(args[1]) ?: run {
                        println("Error: Unknown timeframe: ${args[1]}")
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4")
                        exitProcess(1)
                    }
                ReviewCommand(getBragsUseCase, timeframe, bragPresenter)
            }

            args[0] == "sync-prs" && args.size > 1 -> {
                val timeframe =
                    Timeframe.fromString(args[1]) ?: run {
                        println("Error: Unknown timeframe: ${args[1]}")
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4")
                        exitProcess(1)
                    }
                val printOnly = args.contains("--print-only")
                SyncPullRequestsCommand(syncPullRequestsUseCase, timeframe, printOnly, prSyncPresenter)
            }

            args[0] == "sync-jira" && args.size > 1 -> {
                val timeframe =
                    Timeframe.fromString(args[1]) ?: run {
                        println("Error: Unknown timeframe: ${args[1]}")
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4")
                        exitProcess(1)
                    }
                val printOnly = args.contains("--print-only")
                SyncJiraIssuesCommand(syncJiraIssuesUseCase, timeframe, printOnly, jiraSyncPresenter, userInput)
            }

            args.contains("-c") || args.contains("--comment") -> {
                val commentIndex =
                    if (args.contains("-c")) {
                        args.indexOf("-c")
                    } else {
                        args.indexOf("--comment")
                    }

                if (commentIndex + 1 < args.size) {
                    val comment = args.slice(commentIndex + 1 until args.size).joinToString(" ")
                    AddCommand(addBragUseCase, comment)
                } else {
                    println("Error: No comment provided")
                    exitProcess(1)
                }
            }

            else -> {
                printUsage()
            }
        }

    private fun printUsage(): Nothing {
        println(
            """
            BragDocBuddy - A CLI tool for maintaining a "brag doc document" - your personal record of professional accomplishments.

            Usage:
                BragDocBuddy init                                    Initialize bragging document directory
                BragDocBuddy -c "YOUR TEXT HERE"                     Add a new brag entry
                BragDocBuddy --comment "YOUR TEXT HERE"              Add a new brag entry
                BragDocBuddy about <timeframe>                       Review brags from a time period
                BragDocBuddy sync-prs <timeframe> [--print-only]    Sync merged PRs from GitHub
                BragDocBuddy sync-jira <timeframe> [--print-only]   Sync resolved Jira issues
                BragDocBuddy version                                 Show current version and check for updates

            Timeframes:
                today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4

            Environment Variables:
                BRAG_DOC                          Location of bragging document directory
                BRAG_DOC_REPO_SYNC                Set to 'true' to automatically commit and push to git
                BRAG_DOC_GITHUB_PR_SYNC_ENABLED   Set to 'false' to disable GitHub PR sync (default: true)
                BRAG_DOC_GITHUB_TOKEN             GitHub personal access token (or use 'gh auth login')
                BRAG_DOC_GITHUB_USERNAME          Your GitHub username
                BRAG_DOC_GITHUB_ORG               GitHub organization name
                BRAG_DOC_JIRA_SYNC_ENABLED        Set to 'false' to disable Jira issue sync (default: true)
                BRAG_DOC_JIRA_URL                 Jira URL (e.g., https://your-company.atlassian.net)
                BRAG_DOC_JIRA_EMAIL               Your Jira email address
                BRAG_DOC_JIRA_API_TOKEN           Jira API token
                BRAG_DOC_JIRA_JQL_TEMPLATE        Custom JQL template with {email}, {startDate}, {endDate} placeholders
            """.trimIndent(),
        )
        exitProcess(0)
    }
}
