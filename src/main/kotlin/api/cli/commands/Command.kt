package api.cli.commands

sealed interface Command {
    fun execute()
}
