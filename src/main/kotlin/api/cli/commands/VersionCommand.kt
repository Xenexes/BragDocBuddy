package api.cli.commands

import ports.VersionChecker

class VersionCommand(
    private val versionChecker: VersionChecker,
) : Command {
    override fun execute() {
        versionChecker.checkForUpdates()
    }
}
