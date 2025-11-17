package api.cli.commands

import api.cli.presenters.BragPresenter
import domain.Timeframe
import io.github.oshai.kotlinlogging.KotlinLogging
import usecases.GetBragsUseCase
import kotlin.system.exitProcess

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
