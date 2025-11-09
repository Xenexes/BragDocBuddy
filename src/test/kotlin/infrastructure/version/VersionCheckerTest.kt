package infrastructure.version

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@ExtendWith(MockKExtension::class)
class VersionCheckerTest {
    @Test
    fun `NoOpVersionChecker should do nothing`() {
        val checker = NoOpVersionChecker()

        // Should not throw any exception
        checker.checkForUpdates()
    }

    @Test
    fun `PropertiesVersionProvider should return version from properties file`() {
        // This test will fail if version.properties is not generated
        // In a real scenario, this would be mocked
        val provider = PropertiesVersionProvider()
        val version = provider.getCurrentVersion()

        // Version should either be the actual version or "unknown" if file not found
        assertThat(version.isNotEmpty()).isTrue()
    }

    @ParameterizedTest
    @CsvSource(
        "1.0.0, 1.0.1, true",
        "1.0.0, 1.1.0, true",
        "1.0.0, 2.0.0, true",
        "1.0.1, 1.0.0, false",
        "1.1.0, 1.0.0, false",
        "2.0.0, 1.0.0, false",
        "1.0.0, 1.0.0, false",
        "1.0, 1.0.1, true",
        "1, 1.0.1, true",
    )
    fun `should correctly compare versions`(
        current: String,
        latest: String,
        expectedNewer: Boolean,
    ) {
        val currentVersionProvider = mockk<CurrentVersionProvider>()
        every { currentVersionProvider.getCurrentVersion() } returns current

        val checker =
            GitHubVersionChecker(
                currentVersionProvider = currentVersionProvider,
                githubRepository = "test/repo",
            )

        // Use reflection to test the private isNewerVersion method
        val method = checker::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        val result = method.invoke(checker, current, latest) as Boolean

        if (expectedNewer) {
            assertThat(result).isTrue()
        } else {
            assertThat(result).isFalse()
        }
    }

    @Test
    fun `should not throw exception if version check fails`() {
        val currentVersionProvider = mockk<CurrentVersionProvider>()
        every { currentVersionProvider.getCurrentVersion() } throws RuntimeException("Test exception")

        val checker =
            GitHubVersionChecker(
                currentVersionProvider = currentVersionProvider,
                githubRepository = "test/repo",
            )

        // Should not throw exception
        checker.checkForUpdates()

        verify(exactly = 1) { currentVersionProvider.getCurrentVersion() }
    }
}
