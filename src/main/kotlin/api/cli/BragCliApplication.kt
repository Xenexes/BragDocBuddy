package api.cli

import domain.Timeframe
import infrastructure.version.VersionChecker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import kotlin.system.exitProcess

class BragCliApplication : KoinComponent {
    private val initRepositoryUseCase: InitRepositoryUseCase by inject()
    private val addBragUseCase: AddBragUseCase by inject()
    private val getBragsUseCase: GetBragsUseCase by inject()
    private val versionChecker: VersionChecker by inject()
    private val presenter = Command.BragPresenter()

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
                        println("Valid timeframes: today, yesterday, last-week, last-month, last-year")
                        exitProcess(1)
                    }
                Command.ReviewCommand(getBragsUseCase, timeframe, presenter)
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
            BragLogBuddy - A CLI tool for maintaining a "brag doc document" - your personal record of professional accomplishments.

            Usage:
                BragLogBuddy init                              Initialize bragging document directory
                BragLogBuddy -c "YOUR TEXT HERE"               Add a new brag entry
                BragLogBuddy --comment "YOUR TEXT HERE"        Add a new brag entry
                BragLogBuddy about <timeframe>                 Review brags from a time period
                BragLogBuddy version                           Show current version and check for updates

            Timeframes:
                today, yesterday, last-week, last-month, last-year

            Environment Variables:
                BRAG_LOG            Location of bragging document directory
                BRAG_LOG_REPO_SYNC  Set to 'true' to automatically commit and push to git
            """.trimIndent(),
        )
        exitProcess(0)
    }
}
