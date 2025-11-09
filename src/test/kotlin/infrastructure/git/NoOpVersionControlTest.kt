package infrastructure.git

import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import java.io.File

class NoOpVersionControlTest {
    private val versionControl = NoOpVersionControl()

    @Test
    fun `should always return true for commit and push`() {
        val file = File("/test/path/brags.md")
        val message = "Test commit message"

        val result = versionControl.commitAndPush(file, message)

        assertThat(result).isTrue()
    }

    @Test
    fun `should handle null file without error`() {
        val file = File("")
        val message = "Test message"

        val result = versionControl.commitAndPush(file, message)

        assertThat(result).isTrue()
    }

    @Test
    fun `should handle empty message without error`() {
        val file = File("/test/path/brags.md")
        val message = ""

        val result = versionControl.commitAndPush(file, message)

        assertThat(result).isTrue()
    }

    @Test
    fun `should handle long message without error`() {
        val file = File("/test/path/brags.md")
        val longMessage =
            @Suppress("ktlint:standard:max-line-length")
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.n"

        val result = versionControl.commitAndPush(file, longMessage)

        assertThat(result).isTrue()
    }

    @Test
    fun `should be consistent across multiple calls`() {
        val file = File("/test/path/brags.md")
        val message = "Consistent test message"

        val result1 = versionControl.commitAndPush(file, message)
        val result2 = versionControl.commitAndPush(file, message)
        val result3 = versionControl.commitAndPush(file, message)

        assertThat(result1).isTrue()
        assertThat(result2).isTrue()
        assertThat(result3).isTrue()
    }
}
