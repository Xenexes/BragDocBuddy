package usecases

import domain.BragEntry
import ports.BragRepository
import ports.VersionControl
import java.io.File
import java.time.LocalDateTime

class AddBragUseCase(
    private val repository: BragRepository,
    private val versionControl: VersionControl,
    private val docsLocation: String,
) {
    companion object {
        private const val MAX_COMMIT_MESSAGE_LENGTH = 50
    }

    fun addBragEntry(content: String) {
        if (!repository.isInitialized()) {
            throw IllegalStateException(
                "Repository not initialized. Run 'brag init' first",
            )
        }

        val entry =
            BragEntry(
                timestamp = LocalDateTime.now(),
                content = content,
            )

        repository.save(entry)

        val bragFile = File(docsLocation, "brags.md")
        val commitMessage = buildCommitMessage(content)
        versionControl.commitAndPush(bragFile, commitMessage)
    }

    private fun buildCommitMessage(content: String): String {
        val truncated =
            if (content.length > MAX_COMMIT_MESSAGE_LENGTH) {
                content.take(MAX_COMMIT_MESSAGE_LENGTH) + "..."
            } else {
                content
            }
        return "Add brag: $truncated"
    }
}
