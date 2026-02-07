package api.cli.commands

import api.cli.presenters.BragPresenter
import domain.TimeframeSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import usecases.GetBragsUseCase
import kotlin.system.exitProcess

class ReviewCommand(
    private val useCase: GetBragsUseCase,
    private val timeframeSpec: TimeframeSpec,
    private val presenter: BragPresenter,
) : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        try {
            logger.info { "Retrieving brags for timeframe: $timeframeSpec" }
            val brags = useCase.getBrags(timeframeSpec)
            logger.info { "Found ${brags.values.sumOf { it.size }} brag entries" }
            presenter.present(brags)
        } catch (e: IllegalStateException) {
            logger.error(e) { "Failed to retrieve brags: ${e.message}" }
            System.err.println("Error: ${e.message}")
            exitProcess(1)
        }
    }
}
