package api.cli

import api.cli.commands.AddCommand
import api.cli.commands.InitCommand
import api.cli.commands.ReviewCommand
import api.cli.commands.VersionCommand
import api.cli.presenters.BragPresenter
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import domain.BragEntry
import domain.Timeframe
import domain.TimeframeSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import usecases.AddBragUseCase
import usecases.CheckVersionUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.LocalDateTime

class CommandTest {
    private val originalOut = System.out
    private val testOut = ByteArrayOutputStream()

    @BeforeEach
    fun setUpStreams() {
        System.setOut(PrintStream(testOut))
    }

    @AfterEach
    fun restoreStreams() {
        System.setOut(originalOut)
    }

    private fun getOutput(): String = testOut.toString()

    @Test
    fun `InitCommand should execute successfully`() {
        val useCase = mockk<InitRepositoryUseCase>()
        justRun { useCase.initRepository() }
        val command = InitCommand(useCase)

        command.execute()

        verify { useCase.initRepository() }
        assertThat(getOutput()).contains("Initialized bragging document directory")
    }

    @Test
    fun `AddCommand should execute successfully`() {
        val useCase = mockk<AddBragUseCase>()
        val content = "Completed feature implementation"
        justRun { useCase.addBragEntry(content) }
        val command = AddCommand(useCase, content)

        command.execute()

        verify { useCase.addBragEntry(content) }
        assertThat(getOutput()).contains("Added brag: $content")
    }

    @Test
    fun `VersionCommand should execute successfully`() {
        val checkVersionUseCase = mockk<CheckVersionUseCase>()
        justRun { checkVersionUseCase.checkForUpdates() }
        val command = VersionCommand(checkVersionUseCase)

        command.execute()

        verify { checkVersionUseCase.checkForUpdates() }
    }

    @Test
    fun `ReviewCommand should execute successfully with entries`() {
        val useCase = mockk<GetBragsUseCase>()
        val presenter = BragPresenter()
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
        val entries =
            mapOf(
                "2025-01-15" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 10, 30), "Morning achievement"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 15, 45), "Afternoon achievement"),
                    ),
            )

        every { useCase.getBrags(timeframeSpec) } returns entries
        val command = ReviewCommand(useCase, timeframeSpec, presenter)

        command.execute()

        verify { useCase.getBrags(timeframeSpec) }
        val output = getOutput()
        assertThat(output).contains("2025-01-15")
        assertThat(output).contains("10:30:00 Morning achievement")
        assertThat(output).contains("15:45:00 Afternoon achievement")
    }

    @Test
    fun `ReviewCommand should handle empty results`() {
        val useCase = mockk<GetBragsUseCase>()
        val presenter = BragPresenter()
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.YESTERDAY)
        val emptyEntries = emptyMap<String, List<BragEntry>>()

        every { useCase.getBrags(timeframeSpec) } returns emptyEntries
        val command = ReviewCommand(useCase, timeframeSpec, presenter)

        command.execute()

        verify { useCase.getBrags(timeframeSpec) }
        assertThat(getOutput()).contains("No brags found in this time period.")
    }

    @Test
    fun `BragPresenter should format entries correctly`() {
        val presenter = BragPresenter()
        val entries =
            mapOf(
                "2025-01-15" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 9, 15, 30), "First entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 14, 22, 45), "Second entry"),
                    ),
                "2025-01-16" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 16, 11, 0, 0), "Another day entry"),
                    ),
            )

        presenter.present(entries)

        val output = getOutput()
        assertThat(output).contains("2025-01-15")
        assertThat(output).contains("2025-01-16")
        assertThat(output).contains("09:15:30 First entry")
        assertThat(output).contains("14:22:45 Second entry")
        assertThat(output).contains("11:00:00 Another day entry")
    }

    @Test
    fun `BragPresenter should handle empty entries`() {
        val presenter = BragPresenter()
        val emptyEntries = emptyMap<String, List<BragEntry>>()

        presenter.present(emptyEntries)

        assertThat(getOutput()).contains("No brags found in this time period.")
    }

    @Test
    fun `BragPresenter should format time correctly`() {
        val presenter = BragPresenter()
        val entries =
            mapOf(
                "2025-01-15" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 0, 0, 0), "Midnight entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 12, 0, 0), "Noon entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 23, 59, 59), "Late night entry"),
                    ),
            )

        presenter.present(entries)

        val output = getOutput()
        assertThat(output).contains("00:00:00 Midnight entry")
        assertThat(output).contains("12:00:00 Noon entry")
        assertThat(output).contains("23:59:59 Late night entry")
    }

    @Test
    fun `BragPresenter should handle single entry correctly`() {
        val presenter = BragPresenter()
        val entries =
            mapOf(
                "2025-01-15" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 15, 30, 45), "Single achievement"),
                    ),
            )

        presenter.present(entries)

        val output = getOutput()
        assertThat(output).contains("2025-01-15")
        assertThat(output).contains("15:30:45 Single achievement")
    }

    @Test
    fun `BragPresenter should preserve entry order`() {
        val presenter = BragPresenter()
        val entries =
            mapOf(
                "2025-01-15" to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 8, 0), "First"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 12, 0), "Second"),
                        BragEntry(LocalDateTime.of(2025, 1, 15, 16, 0), "Third"),
                    ),
            )

        presenter.present(entries)

        val output = getOutput()
        val lines = output.lines()
        val firstIndex = lines.indexOfFirst { it.contains("First") }
        val secondIndex = lines.indexOfFirst { it.contains("Second") }
        val thirdIndex = lines.indexOfFirst { it.contains("Third") }

        assertThat(firstIndex < secondIndex).isTrue()
        assertThat(secondIndex < thirdIndex).isTrue()
    }
}
