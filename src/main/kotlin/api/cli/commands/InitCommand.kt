package api.cli.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import usecases.InitRepositoryUseCase
import kotlin.system.exitProcess

class InitCommand(
    private val useCase: InitRepositoryUseCase,
) : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        try {
            logger.info { "Initializing repository" }
            useCase.initRepository()
            println("Initialized bragging document directory")
            logger.info { "Repository initialized successfully" }
        } catch (e: IllegalStateException) {
            logger.error(e) { "Failed to initialize repository: ${e.message}" }
            System.err.println("Error: ${e.message}")
            exitProcess(1)
        }
    }
}
