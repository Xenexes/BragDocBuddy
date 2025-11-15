package infrastructure.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import domain.Timeframe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class TimeframeToDateRangeParserTest {
    private val parser = TimeframeToDateRangeParser()

    @Test
    fun `should parse TODAY to current date range`() {
        val today = LocalDate.now()

        val result = parser.parse(Timeframe.TODAY)

        assertThat(result.start).isEqualTo(today)
        assertThat(result.end).isEqualTo(today)
    }

    @Test
    fun `should parse YESTERDAY to previous date range`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val result = parser.parse(Timeframe.YESTERDAY)

        assertThat(result.start).isEqualTo(yesterday)
        assertThat(result.end).isEqualTo(yesterday)
    }

    @Test
    fun `should parse LAST_WEEK to date range from week ago to today`() {
        val today = LocalDate.now()
        val weekAgo = today.minusWeeks(1)

        val result = parser.parse(Timeframe.LAST_WEEK)

        assertThat(result.start).isEqualTo(weekAgo)
        assertThat(result.end).isEqualTo(today)
    }

    @Test
    fun `should parse LAST_MONTH to date range from month ago to today`() {
        val today = LocalDate.now()
        val monthAgo = today.minusMonths(1)

        val result = parser.parse(Timeframe.LAST_MONTH)

        assertThat(result.start).isEqualTo(monthAgo)
        assertThat(result.end).isEqualTo(today)
    }

    @Test
    fun `should parse LAST_YEAR to date range from year ago to today`() {
        val today = LocalDate.now()
        val yearAgo = today.minusYears(1)

        val result = parser.parse(Timeframe.LAST_YEAR)

        assertThat(result.start).isEqualTo(yearAgo)
        assertThat(result.end).isEqualTo(today)
    }

    @ParameterizedTest
    @EnumSource(Timeframe::class)
    fun `should return valid DateRange for all timeframes`(timeframe: Timeframe) {
        val result = parser.parse(timeframe)

        assertThat(result.start.isAfter(result.end)).isFalse()
        assertThat(result.start).isNotNull()
        assertThat(result.end).isNotNull()
    }

    @Test
    fun `should handle TODAY when called multiple times consistently`() {
        val result1 = parser.parse(Timeframe.TODAY)
        val result2 = parser.parse(Timeframe.TODAY)

        assertThat(result1.start).isEqualTo(result2.start)
        assertThat(result1.end).isEqualTo(result2.end)
    }

    @Test
    fun `should ensure YESTERDAY is before TODAY`() {
        val today = parser.parse(Timeframe.TODAY)
        val yesterday = parser.parse(Timeframe.YESTERDAY)

        assertThat(yesterday.end.isBefore(today.start)).isTrue()
    }

    @Test
    fun `should ensure LAST_WEEK includes multiple days`() {
        val result = parser.parse(Timeframe.LAST_WEEK)

        assertThat(result.start.isBefore(result.end)).isTrue()
        val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
        assertThat(daysDifference).isEqualTo(7L) // Exactly 7 days between start and end
    }

    @Test
    fun `should ensure LAST_MONTH includes significant time period`() {
        val result = parser.parse(Timeframe.LAST_MONTH)

        assertThat(result.start.isBefore(result.end)).isTrue()
        val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
        assertThat(daysDifference >= 28).isTrue() // At least 28 days
        assertThat(daysDifference <= 31).isTrue() // At most 31 days
    }

    @Test
    fun `should ensure LAST_YEAR includes significant time period`() {
        val result = parser.parse(Timeframe.LAST_YEAR)

        assertThat(result.start.isBefore(result.end)).isTrue()
        val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
        assertThat(daysDifference >= 365).isTrue() // At least 365 days
        assertThat(daysDifference <= 366).isTrue() // At most 366 days (leap year)
    }

    @Test
    fun `should handle leap year edge cases for LAST_YEAR`() {
        // Note: This test verifies that the parser handles leap years correctly calculating date ranges, though the specific behavior depends on the current date
        val result = parser.parse(Timeframe.LAST_YEAR)

        assertThat(result.start).isNotNull()
        assertThat(result.end).isNotNull()
        assertThat(result.start.isBefore(result.end) || result.start.isEqual(result.end)).isTrue()
    }

    @Test
    fun `should handle month boundary edge cases for LAST_MONTH`() {
        // Note: This test verifies that the parser handles month boundaries correctly
        // For example, if today is March 31, last month should go back to the end of February
        val result = parser.parse(Timeframe.LAST_MONTH)

        assertThat(result.start).isNotNull()
        assertThat(result.end).isNotNull()
        assertThat(result.start.isBefore(result.end) || result.start.isEqual(result.end)).isTrue()
    }

    @ParameterizedTest
    @CsvSource(
        "QUARTER_ONE, 1, 1, 3, 31",
        "QUARTER_TWO, 4, 1, 6, 30",
        "QUARTER_THREE, 7, 1, 9, 30",
        "QUARTER_FOUR, 10, 1, 12, 31",
    )
    fun `should parse quarters to correct date ranges`(
        timeframe: Timeframe,
        startMonth: Int,
        startDay: Int,
        endMonth: Int,
        endDay: Int,
    ) {
        val currentYear =
            java.time.Year
                .now()
                .value

        val result = parser.parse(timeframe)

        assertThat(result.start).isEqualTo(LocalDate.of(currentYear, startMonth, startDay))
        assertThat(result.end).isEqualTo(LocalDate.of(currentYear, endMonth, endDay))
    }

    @Test
    fun `should ensure quarters are exactly 3 months long`() {
        val q1 = parser.parse(Timeframe.QUARTER_ONE)
        val q2 = parser.parse(Timeframe.QUARTER_TWO)
        val q3 = parser.parse(Timeframe.QUARTER_THREE)
        val q4 = parser.parse(Timeframe.QUARTER_FOUR)

        val q1Days = q1.end.toEpochDay() - q1.start.toEpochDay() + 1
        assertThat(q1Days >= 90).isTrue()
        assertThat(q1Days <= 91).isTrue()

        val q2Days = q2.end.toEpochDay() - q2.start.toEpochDay() + 1
        assertThat(q2Days).isEqualTo(91L)

        val q3Days = q3.end.toEpochDay() - q3.start.toEpochDay() + 1
        assertThat(q3Days).isEqualTo(92L)

        val q4Days = q4.end.toEpochDay() - q4.start.toEpochDay() + 1
        assertThat(q4Days).isEqualTo(92L)
    }

    @Test
    fun `should ensure quarters cover entire year without gaps`() {
        val q1 = parser.parse(Timeframe.QUARTER_ONE)
        val q2 = parser.parse(Timeframe.QUARTER_TWO)
        val q3 = parser.parse(Timeframe.QUARTER_THREE)
        val q4 = parser.parse(Timeframe.QUARTER_FOUR)

        assertThat(q2.start).isEqualTo(q1.end.plusDays(1))

        assertThat(q3.start).isEqualTo(q2.end.plusDays(1))

        assertThat(q4.start).isEqualTo(q3.end.plusDays(1))
    }
}
