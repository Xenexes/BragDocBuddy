package api.cli

import domain.Timeframe
import infrastructure.version.VersionChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import usecases.SyncPullRequestsUseCase
import kotlin.system.exitProcess

class BragCliApplication : KoinComponent {
    private val initRepositoryUseCase: InitRepositoryUseCase by inject()
    private val addBragUseCase: AddBragUseCase by inject()
    private val getBragsUseCase: GetBragsUseCase by inject()
    private val syncPullRequestsUseCase: SyncPullRequestsUseCase by inject()
    private val versionChecker: VersionChecker by inject()
    private val userInput: UserInput by inject()
    private val bragPresenter = Command.BragPresenter()
    private val prSyncPresenter = Command.PullRequestSyncPresenter()

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
                Command.InitCommand(initRepositoryUseCase)
            }

            args[0] == "version" -> {
                Command.VersionCommand(versionChecker)
            }

            args[0] == "about" && args.size > 1 -> {
                val timeframe =
                    Timeframe.fromString(args[1]) ?: run {
                        println("Error: Unknown timeframe: ${args[1]}")
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4")
                        exitProcess(1)
                    }
                Command.ReviewCommand(getBragsUseCase, timeframe, bragPresenter)
            }

            args[0] == "sync-prs" && args.size > 1 -> {
                val timeframe =
                    Timeframe.fromString(args[1]) ?: run {
                        println("Error: Unknown timeframe: ${args[1]}")
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4")
                        exitProcess(1)
                    }
                val printOnly = args.contains("--print-only")
                Command.SyncPullRequestsCommand(syncPullRequestsUseCase, timeframe, printOnly, prSyncPresenter)
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
                    Command.AddCommand(addBragUseCase, comment)
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
                BragDocBuddy init                                 Initialize bragging document directory
                BragDocBuddy -c "YOUR TEXT HERE"                  Add a new brag entry
                BragDocBuddy --comment "YOUR TEXT HERE"           Add a new brag entry
                BragDocBuddy about <timeframe>                    Review brags from a time period
                BragDocBuddy sync-prs <timeframe> [--print-only]  Sync merged PRs from GitHub
                BragDocBuddy version                              Show current version and check for updates

            Timeframes:
                today, yesterday, last-week, last-month, last-year, q1, q2, q3, q4

            Environment Variables:
                BRAG_DOC            Location of bragging document directory
                BRAG_DOC_REPO_SYNC  Set to 'true' to automatically commit and push to git
                BRAG_DOC                          Location of bragging document directory
                BRAG_DOC_REPO_SYNC                Set to 'true' to automatically commit and push to git
                BRAG_DOC_GITHUB_PR_SYNC_ENABLED   Set to 'false' to disable GitHub PR sync (default: true)
                BRAG_DOC_GITHUB_TOKEN             GitHub personal access token (or use 'gh auth login')
                BRAG_DOC_GITHUB_USERNAME          Your GitHub username
                BRAG_DOC_GITHUB_ORG               GitHub organization name
            """.trimIndent(),
        )
        exitProcess(0)
    }
}
