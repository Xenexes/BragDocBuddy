package api.cli.commands

import io.github.oshai.kotlinlogging.KotlinLogging
import usecases.AddBragUseCase
import kotlin.system.exitProcess

class AddCommand(
    private val useCase: AddBragUseCase,
    private val content: String,
) : Command {
    private val logger = KotlinLogging.logger {}

    override fun execute() {
        try {
            logger.info { "Adding brag entry" }
            useCase.addBragEntry(content)
            println("Added brag: $content")
            logger.info { "Brag entry added successfully" }
        } catch (e: IllegalStateException) {
            logger.error(e) { "Failed to add brag entry: ${e.message}" }
            System.err.println("Error: ${e.message}")
            exitProcess(1)
        }
    }
}
