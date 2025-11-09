package usecases

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import domain.BragEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ports.BragRepository
import ports.VersionControl
import java.io.File
import java.time.LocalDateTime

class AddBragUseCaseTest {
    private val repository = mockk<BragRepository>()
    private val versionControl = mockk<VersionControl>()
    private val docsLocation = "/test/path"
    private val useCase = AddBragUseCase(repository, versionControl, docsLocation)

    @Test
    fun `should add brag entry successfully when repository is initialized`() {
        val content = "Completed feature implementation"
        val capturedEntry = slot<BragEntry>()
        val capturedFile = slot<File>()
        val capturedMessage = slot<String>()

        every { repository.isInitialized() } returns true
        every { repository.save(capture(capturedEntry)) } returns Unit
        every { versionControl.commitAndPush(capture(capturedFile), capture(capturedMessage)) } returns true

        useCase.addBragEntry(content)

        verify { repository.save(any()) }
        verify { versionControl.commitAndPush(any(), any()) }

        assertThat(capturedEntry.captured.content).isEqualTo(content)
        assertThat(capturedEntry.captured.timestamp).isNotNull()
        assertThat(capturedFile.captured.path).isEqualTo(File("/test/path/brags.md").path)
        assertThat(capturedMessage.captured).isEqualTo("Add brag: Completed feature implementation")
    }

    @Test
    fun `should throw exception when repository is not initialized`() {
        val content = "Test content"
        every { repository.isInitialized() } returns false

        val exception =
            assertThrows<IllegalStateException> {
                useCase.addBragEntry(content)
            }

        assertThat(exception).hasMessage("Repository not initialized. Run 'brag init' first")
        verify(exactly = 0) { repository.save(any()) }
        verify(exactly = 0) { versionControl.commitAndPush(any(), any()) }
    }

    @Test
    fun `should truncate long content in commit message`() {
        val longContent =
            @Suppress("ktlint:standard:max-line-length")
            "This is a very long brag entry that contains more than 50 characters and should be truncated in the commit message"
        val capturedMessage = slot<String>()

        every { repository.isInitialized() } returns true
        every { repository.save(any()) } returns Unit
        every { versionControl.commitAndPush(any(), capture(capturedMessage)) } returns true

        useCase.addBragEntry(longContent)

        val expectedMessage = "Add brag: This is a very long brag entry that contains more ..."
        assertThat(capturedMessage.captured).isEqualTo(expectedMessage)
    }

    @Test
    fun `should not truncate short content in commit message`() {
        val shortContent = "Short content"
        val capturedMessage = slot<String>()

        every { repository.isInitialized() } returns true
        every { repository.save(any()) } returns Unit
        every { versionControl.commitAndPush(any(), capture(capturedMessage)) } returns true

        useCase.addBragEntry(shortContent)

        val expectedMessage = "Add brag: Short content"
        assertThat(capturedMessage.captured).isEqualTo(expectedMessage)
    }

    @Test
    fun `should handle exactly 50 character content without truncation`() {
        val exactContent = "12345678901234567890123456789012345678901234567890" // Exactly 50 chars
        val capturedMessage = slot<String>()

        every { repository.isInitialized() } returns true
        every { repository.save(any()) } returns Unit
        every { versionControl.commitAndPush(any(), capture(capturedMessage)) } returns true

        useCase.addBragEntry(exactContent)

        val expectedMessage = "Add brag: 12345678901234567890123456789012345678901234567890"
        assertThat(capturedMessage.captured).isEqualTo(expectedMessage)
    }

    @Test
    fun `should handle empty content`() {
        val emptyContent = ""
        val capturedEntry = slot<BragEntry>()
        val capturedMessage = slot<String>()

        every { repository.isInitialized() } returns true
        every { repository.save(capture(capturedEntry)) } returns Unit
        every { versionControl.commitAndPush(any(), capture(capturedMessage)) } returns true

        useCase.addBragEntry(emptyContent)

        assertThat(capturedEntry.captured.content).isEqualTo("")
        assertThat(capturedMessage.captured).isEqualTo("Add brag: ")
    }

    @Test
    fun `should create brag entry with current timestamp`() {
        val content = "Test content"
        val capturedEntry = slot<BragEntry>()
        val beforeTimestamp = LocalDateTime.now()

        every { repository.isInitialized() } returns true
        every { repository.save(capture(capturedEntry)) } returns Unit
        every { versionControl.commitAndPush(any(), any()) } returns true

        useCase.addBragEntry(content)
        val afterTimestamp = LocalDateTime.now()

        val entryTimestamp = capturedEntry.captured.timestamp
        assertThat(entryTimestamp.isAfter(beforeTimestamp.minusSeconds(1))).isTrue()
        assertThat(entryTimestamp.isBefore(afterTimestamp.plusSeconds(1))).isTrue()
    }

    @Test
    fun `should use correct file path for version control`() {
        val content = "Test content"
        val capturedFile = slot<File>()

        every { repository.isInitialized() } returns true
        every { repository.save(any()) } returns Unit
        every { versionControl.commitAndPush(capture(capturedFile), any()) } returns true

        useCase.addBragEntry(content)

        assertThat(capturedFile.captured.parent).isEqualTo(File(docsLocation).path)
        assertThat(capturedFile.captured.name).isEqualTo("brags.md")
    }
}
