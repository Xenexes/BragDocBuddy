package api.cli.commands

import api.cli.presenters.PullRequestSyncPresenter
import domain.TimeframeSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import usecases.SyncPullRequestsUseCase
import kotlin.system.exitProcess

class SyncPullRequestsCommand(
    private val useCase: SyncPullRequestsUseCase,
    private val timeframeSpec: TimeframeSpec,
    private val printOnly: Boolean,
    private val presenter: PullRequestSyncPresenter,
) : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        runBlocking {
            try {
                logger.info { "Executing sync pull requests command (timeframe: $timeframeSpec, printOnly: $printOnly)" }
                val result = useCase.syncPullRequests(timeframeSpec, printOnly)
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
