package api.cli.commands

import infrastructure.version.VersionChecker

class VersionCommand(
    private val versionChecker: VersionChecker,
) : Command {
    override fun execute() {
        versionChecker.checkForUpdates()
    }
}
