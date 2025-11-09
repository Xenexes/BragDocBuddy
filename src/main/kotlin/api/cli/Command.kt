package api.cli

import domain.BragEntry
import domain.Timeframe
import infrastructure.version.VersionChecker
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
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
}
