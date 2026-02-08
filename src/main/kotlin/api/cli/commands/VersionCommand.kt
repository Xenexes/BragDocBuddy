package api.cli.commands

import usecases.CheckVersionUseCase

class VersionCommand(
    private val checkVersionUseCase: CheckVersionUseCase,
) : Command {
    override fun execute() {
        checkVersionUseCase.checkForUpdates()
    }
}
