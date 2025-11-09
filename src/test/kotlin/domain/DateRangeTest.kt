package domain

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class DateRangeTest {
    @Test
    fun `should create date range with valid start and end dates`() {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)

        val dateRange = DateRange(start, end)

        assertThat(dateRange.start).isEqualTo(start)
        assertThat(dateRange.end).isEqualTo(end)
    }

    @Test
    fun `should allow same start and end dates`() {
        val date = LocalDate.of(2024, 1, 15)

        val dateRange = DateRange(date, date)

        assertThat(dateRange.start).isEqualTo(date)
        assertThat(dateRange.end).isEqualTo(date)
    }

    @Test
    fun `should throw exception when start date is after end date`() {
        val start = LocalDate.of(2025, 1, 31)
        val end = LocalDate.of(2025, 1, 1)

        val exception =
            assertThrows<IllegalArgumentException> {
                DateRange(start, end)
            }

        assertThat(exception).hasMessage("Start date must be before or equal to end date")
    }

    @Test
    fun `should contain date within range`() {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)
        val dateRange = DateRange(start, end)
        val dateInRange = LocalDate.of(2025, 1, 15)

        val result = dateRange.contains(dateInRange)

        assertThat(result).isTrue()
    }

    @Test
    fun `should contain start date`() {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)
        val dateRange = DateRange(start, end)

        val result = dateRange.contains(start)

        assertThat(result).isTrue()
    }

    @Test
    fun `should contain end date`() {
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)
        val dateRange = DateRange(start, end)

        val result = dateRange.contains(end)

        assertThat(result).isTrue()
    }

    @Test
    fun `should not contain date before range`() {
        val start = LocalDate.of(2025, 1, 10)
        val end = LocalDate.of(2025, 1, 20)
        val dateRange = DateRange(start, end)
        val dateBeforeRange = LocalDate.of(2025, 1, 5)

        val result = dateRange.contains(dateBeforeRange)

        assertThat(result).isFalse()
    }

    @Test
    fun `should not contain date after range`() {
        val start = LocalDate.of(2025, 1, 10)
        val end = LocalDate.of(2025, 1, 20)
        val dateRange = DateRange(start, end)
        val dateAfterRange = LocalDate.of(2025, 1, 25)

        val result = dateRange.contains(dateAfterRange)

        assertThat(result).isFalse()
    }

    @Test
    fun `should handle single day range`() {
        val singleDate = LocalDate.of(2025, 1, 15)
        val dateRange = DateRange(singleDate, singleDate)

        assertThat(dateRange.contains(singleDate)).isTrue()
        assertThat(dateRange.contains(singleDate.minusDays(1))).isFalse()
        assertThat(dateRange.contains(singleDate.plusDays(1))).isFalse()
    }

    @Test
    fun `should handle leap year dates`() {
        // (2024 is a leap year)
        val start = LocalDate.of(2024, 2, 28)
        val end = LocalDate.of(2024, 3, 1)
        val dateRange = DateRange(start, end)
        val leapDay = LocalDate.of(2024, 2, 29)

        val result = dateRange.contains(leapDay)

        assertThat(result).isTrue()
    }

    @Test
    fun `should handle cross-year date range`() {
        val start = LocalDate.of(2024, 12, 15)
        val end = LocalDate.of(2025, 1, 15)
        val dateRange = DateRange(start, end)
        val newYearDate = LocalDate.of(2025, 1, 1)

        val result = dateRange.contains(newYearDate)

        assertThat(result).isTrue()
    }
}
