package usecases

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.key
import domain.BragEntry
import domain.DateRange
import domain.Timeframe
import domain.TimeframeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ports.BragRepository
import ports.TimeframeParser
import java.time.LocalDate
import java.time.LocalDateTime

class GetBragsUseCaseTest {
    private val repository = mockk<BragRepository>()
    private val timeframeParser = mockk<TimeframeParser>()
    private val useCase = GetBragsUseCase(repository, timeframeParser)

    @Test
    fun `should get brags successfully when repository is initialized`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
        val dateRange = DateRange(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15))
        val entries =
            listOf(
                BragEntry(LocalDateTime.of(2024, 1, 15, 10, 0), "Morning achievement"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 15, 0), "Afternoon achievement"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns entries

        val result = useCase.getBrags(timeframeSpec)

        verify { timeframeParser.parse(timeframeSpec) }
        verify { repository.findByDateRange(dateRange) }

        assertThat(result).hasSize(1)
        assertThat(result).key("2024-01-15").isEqualTo(entries)
    }

    @Test
    fun `should throw exception when repository is not initialized`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
        every { repository.isInitialized() } returns false

        val exception =
            assertThrows<IllegalStateException> {
                useCase.getBrags(timeframeSpec)
            }

        assertThat(exception).hasMessage("Repository not initialized. Run 'brag init' first")
        verify(exactly = 0) { timeframeParser.parse(any()) }
        verify(exactly = 0) { repository.findByDateRange(any()) }
    }

    @Test
    fun `should return empty map when no entries found`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.YESTERDAY)
        val dateRange = DateRange(LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 14))
        val emptyEntries = emptyList<BragEntry>()

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns emptyEntries

        val result = useCase.getBrags(timeframeSpec)

        assertThat(result).hasSize(0)
    }

    @Test
    fun `should group entries by date correctly`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_WEEK)
        val dateRange = DateRange(LocalDate.of(2024, 1, 8), LocalDate.of(2024, 1, 15))
        val entries =
            listOf(
                BragEntry(LocalDateTime.of(2024, 1, 14, 10, 0), "Day 1 - Morning"),
                BragEntry(LocalDateTime.of(2024, 1, 14, 15, 0), "Day 1 - Afternoon"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 9, 0), "Day 2 - Morning"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 14, 0), "Day 2 - Afternoon"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 18, 0), "Day 2 - Evening"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns entries

        val result = useCase.getBrags(timeframeSpec)

        assertThat(result).hasSize(2)
        assertThat(result.keys).isEqualTo(setOf("2024-01-14", "2024-01-15"))

        val day14Entries = result["2024-01-14"]!!
        assertThat(day14Entries).hasSize(2)
        assertThat(day14Entries[0]).isEqualTo(BragEntry(LocalDateTime.of(2024, 1, 14, 10, 0), "Day 1 - Morning"))
        assertThat(day14Entries[1]).isEqualTo(BragEntry(LocalDateTime.of(2024, 1, 14, 15, 0), "Day 1 - Afternoon"))

        val day15Entries = result["2024-01-15"]!!
        assertThat(day15Entries).hasSize(3)
        assertThat(day15Entries[0]).isEqualTo(BragEntry(LocalDateTime.of(2024, 1, 15, 9, 0), "Day 2 - Morning"))
        assertThat(day15Entries[1]).isEqualTo(BragEntry(LocalDateTime.of(2024, 1, 15, 14, 0), "Day 2 - Afternoon"))
        assertThat(day15Entries[2]).isEqualTo(BragEntry(LocalDateTime.of(2024, 1, 15, 18, 0), "Day 2 - Evening"))
    }

    @Test
    fun `should handle single entry correctly`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
        val dateRange = DateRange(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15))
        val singleEntry =
            listOf(
                BragEntry(LocalDateTime.of(2024, 1, 15, 12, 0), "Single achievement"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns singleEntry

        val result = useCase.getBrags(timeframeSpec)

        assertThat(result).hasSize(1)
        assertThat(result).key("2024-01-15").hasSize(1)
        assertThat(result["2024-01-15"]!![0].content).isEqualTo("Single achievement")
    }

    @Test
    fun `should handle all timeframe types`() {
        val timeframeSpecs =
            listOf(
                TimeframeSpec.Predefined(Timeframe.TODAY),
                TimeframeSpec.Predefined(Timeframe.YESTERDAY),
                TimeframeSpec.Predefined(Timeframe.LAST_WEEK),
                TimeframeSpec.Predefined(Timeframe.LAST_MONTH),
                TimeframeSpec.Predefined(Timeframe.LAST_YEAR),
            )

        every { repository.isInitialized() } returns true
        every { repository.findByDateRange(any()) } returns emptyList()

        timeframeSpecs.forEach { timeframeSpec ->
            val mockDateRange = DateRange(LocalDate.now(), LocalDate.now())
            every { timeframeParser.parse(timeframeSpec) } returns mockDateRange
        }

        timeframeSpecs.forEach { timeframeSpec ->
            val result = useCase.getBrags(timeframeSpec)
            assertThat(result).hasSize(0)
            verify { timeframeParser.parse(timeframeSpec) }
        }
    }

    @Test
    fun `should preserve entry order within same date group`() {
        val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
        val dateRange = DateRange(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 15))
        val entries =
            listOf(
                BragEntry(LocalDateTime.of(2024, 1, 15, 8, 0), "First entry"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 12, 0), "Second entry"),
                BragEntry(LocalDateTime.of(2024, 1, 15, 16, 0), "Third entry"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns entries

        val result = useCase.getBrags(timeframeSpec)

        val dayEntries = result["2024-01-15"]!!
        assertThat(dayEntries[0].content).isEqualTo("First entry")
        assertThat(dayEntries[1].content).isEqualTo("Second entry")
        assertThat(dayEntries[2].content).isEqualTo("Third entry")
    }

    @Test
    fun `should handle quarter with year timeframe`() {
        val timeframeSpec = TimeframeSpec.QuarterWithYear(1, 2025)
        val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31))
        val entries =
            listOf(
                BragEntry(LocalDateTime.of(2025, 2, 15, 10, 0), "Q1 achievement"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns entries

        val result = useCase.getBrags(timeframeSpec)

        assertThat(result).hasSize(1)
        assertThat(result).key("2025-02-15").hasSize(1)
    }

    @Test
    fun `should handle custom date range timeframe`() {
        val start = LocalDate.of(2024, 12, 1)
        val end = LocalDate.of(2025, 2, 28)
        val timeframeSpec = TimeframeSpec.Custom(start, end)
        val dateRange = DateRange(start, end)
        val entries =
            listOf(
                BragEntry(LocalDateTime.of(2025, 1, 15, 10, 0), "Custom range achievement"),
            )

        every { repository.isInitialized() } returns true
        every { timeframeParser.parse(timeframeSpec) } returns dateRange
        every { repository.findByDateRange(dateRange) } returns entries

        val result = useCase.getBrags(timeframeSpec)

        assertThat(result).hasSize(1)
        assertThat(result).key("2025-01-15").hasSize(1)
    }
}
