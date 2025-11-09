package infrastructure.git

import ports.VersionControl
import java.io.File

class GitVersionControl(
    private val repositoryPath: String,
) : VersionControl {
    private val repoDir: File
        get() = File(repositoryPath)

    override fun commitAndPush(
        file: File,
        message: String,
    ): Boolean =
        try {
            executeGitCommand("add", file.name)
            executeGitCommand("commit", "-m", message)
            val exitCode = executeGitCommand("push")
            exitCode == 0
        } catch (e: Exception) {
            false
        }

    private fun executeGitCommand(vararg args: String): Int {
        val command = listOf("git") + args
        val process =
            ProcessBuilder(command)
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()

        return process.waitFor()
    }
}
