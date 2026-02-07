package api.cli.commands

import api.cli.presenters.JiraIssueSyncPresenter
import domain.JiraIssueSyncResult
import domain.TimeframeSpec
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import ports.UserInput
import usecases.SyncJiraIssuesUseCase
import kotlin.system.exitProcess

class SyncJiraIssuesCommand(
    private val useCase: SyncJiraIssuesUseCase,
    private val timeframeSpec: TimeframeSpec,
    private val printOnly: Boolean,
    private val presenter: JiraIssueSyncPresenter,
    private val userInput: UserInput,
) : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        runBlocking {
            try {
                logger.info { "Executing sync Jira issues command (timeframe: $timeframeSpec, printOnly: $printOnly)" }
                when (val result = useCase.syncJiraIssues(timeframeSpec, printOnly)) {
                    is JiraIssueSyncResult.ReadyToSync -> {
                        logger.info { "Ready to sync ${result.issues.size} Jira issues, waiting for user selection" }
                        presenter.presentInteractive(result.issues, userInput) { selectedIssues ->
                            logger.info { "User selected ${selectedIssues.size} issues to sync" }
                            val syncResult = useCase.syncSelectedIssues(selectedIssues)
                            presenter.presentSyncResult(syncResult)
                        }
                    }
                    else -> presenter.present(result)
                }
                logger.info { "Jira issues sync command completed successfully" }
            } catch (e: IllegalStateException) {
                logger.error(e) { "Failed to sync Jira issues: ${e.message}" }
                System.err.println("Error: ${e.message}")
                exitProcess(1)
            }
        }
    }
}
