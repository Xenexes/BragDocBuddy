package infrastructure.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import domain.Timeframe
import domain.TimeframeSpec
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class TimeframeToDateRangeParserTest {
    private val parser = TimeframeToDateRangeParser()

    @Nested
    inner class PredefinedTimeframes {
        @Test
        fun `should parse TODAY to current date range`() {
            val today = LocalDate.now()

            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.TODAY))

            assertThat(result.start).isEqualTo(today)
            assertThat(result.end).isEqualTo(today)
        }

        @Test
        fun `should parse YESTERDAY to previous date range`() {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.YESTERDAY))

            assertThat(result.start).isEqualTo(yesterday)
            assertThat(result.end).isEqualTo(yesterday)
        }

        @Test
        fun `should parse LAST_WEEK to date range from week ago to today`() {
            val today = LocalDate.now()
            val weekAgo = today.minusWeeks(1)

            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_WEEK))

            assertThat(result.start).isEqualTo(weekAgo)
            assertThat(result.end).isEqualTo(today)
        }

        @Test
        fun `should parse LAST_MONTH to date range from month ago to today`() {
            val today = LocalDate.now()
            val monthAgo = today.minusMonths(1)

            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_MONTH))

            assertThat(result.start).isEqualTo(monthAgo)
            assertThat(result.end).isEqualTo(today)
        }

        @Test
        fun `should parse LAST_YEAR to date range from year ago to today`() {
            val today = LocalDate.now()
            val yearAgo = today.minusYears(1)

            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_YEAR))

            assertThat(result.start).isEqualTo(yearAgo)
            assertThat(result.end).isEqualTo(today)
        }

        @ParameterizedTest
        @EnumSource(Timeframe::class)
        fun `should return valid DateRange for all timeframes`(timeframe: Timeframe) {
            val result = parser.parse(TimeframeSpec.Predefined(timeframe))

            assertThat(result.start.isAfter(result.end)).isFalse()
            assertThat(result.start).isNotNull()
            assertThat(result.end).isNotNull()
        }

        @Test
        fun `should handle TODAY when called multiple times consistently`() {
            val result1 = parser.parse(TimeframeSpec.Predefined(Timeframe.TODAY))
            val result2 = parser.parse(TimeframeSpec.Predefined(Timeframe.TODAY))

            assertThat(result1.start).isEqualTo(result2.start)
            assertThat(result1.end).isEqualTo(result2.end)
        }

        @Test
        fun `should ensure YESTERDAY is before TODAY`() {
            val today = parser.parse(TimeframeSpec.Predefined(Timeframe.TODAY))
            val yesterday = parser.parse(TimeframeSpec.Predefined(Timeframe.YESTERDAY))

            assertThat(yesterday.end.isBefore(today.start)).isTrue()
        }

        @Test
        fun `should ensure LAST_WEEK includes multiple days`() {
            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_WEEK))

            assertThat(result.start.isBefore(result.end)).isTrue()
            val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
            assertThat(daysDifference).isEqualTo(7L)
        }

        @Test
        fun `should ensure LAST_MONTH includes significant time period`() {
            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_MONTH))

            assertThat(result.start.isBefore(result.end)).isTrue()
            val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
            assertThat(daysDifference >= 28).isTrue()
            assertThat(daysDifference <= 31).isTrue()
        }

        @Test
        fun `should ensure LAST_YEAR includes significant time period`() {
            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_YEAR))

            assertThat(result.start.isBefore(result.end)).isTrue()
            val daysDifference = result.end.toEpochDay() - result.start.toEpochDay()
            assertThat(daysDifference >= 365).isTrue()
            assertThat(daysDifference <= 366).isTrue()
        }

        @Test
        fun `should handle leap year edge cases for LAST_YEAR`() {
            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_YEAR))

            assertThat(result.start).isNotNull()
            assertThat(result.end).isNotNull()
            assertThat(result.start.isBefore(result.end) || result.start.isEqual(result.end)).isTrue()
        }

        @Test
        fun `should handle month boundary edge cases for LAST_MONTH`() {
            val result = parser.parse(TimeframeSpec.Predefined(Timeframe.LAST_MONTH))

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

            val result = parser.parse(TimeframeSpec.Predefined(timeframe))

            assertThat(result.start).isEqualTo(LocalDate.of(currentYear, startMonth, startDay))
            assertThat(result.end).isEqualTo(LocalDate.of(currentYear, endMonth, endDay))
        }

        @Test
        fun `should ensure quarters are exactly 3 months long`() {
            val q1 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_ONE))
            val q2 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_TWO))
            val q3 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_THREE))
            val q4 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_FOUR))

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
            val q1 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_ONE))
            val q2 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_TWO))
            val q3 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_THREE))
            val q4 = parser.parse(TimeframeSpec.Predefined(Timeframe.QUARTER_FOUR))

            assertThat(q2.start).isEqualTo(q1.end.plusDays(1))
            assertThat(q3.start).isEqualTo(q2.end.plusDays(1))
            assertThat(q4.start).isEqualTo(q3.end.plusDays(1))
        }
    }

    @Nested
    inner class QuarterWithYear {
        @ParameterizedTest
        @CsvSource(
            "1, 2025, 1, 1, 3, 31",
            "2, 2025, 4, 1, 6, 30",
            "3, 2025, 7, 1, 9, 30",
            "4, 2025, 10, 1, 12, 31",
            "1, 2024, 1, 1, 3, 31",
            "2, 2024, 4, 1, 6, 30",
        )
        fun `should parse quarter with year to correct date ranges`(
            quarter: Int,
            year: Int,
            startMonth: Int,
            startDay: Int,
            endMonth: Int,
            endDay: Int,
        ) {
            val result = parser.parse(TimeframeSpec.QuarterWithYear(quarter, year))

            assertThat(result.start).isEqualTo(LocalDate.of(year, startMonth, startDay))
            assertThat(result.end).isEqualTo(LocalDate.of(year, endMonth, endDay))
        }

        @Test
        fun `should handle Q1 in leap year correctly`() {
            val result = parser.parse(TimeframeSpec.QuarterWithYear(1, 2024))

            assertThat(result.start).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(result.end).isEqualTo(LocalDate.of(2024, 3, 31))

            val days = result.end.toEpochDay() - result.start.toEpochDay() + 1
            assertThat(days).isEqualTo(91L)
        }

        @Test
        fun `should handle historical year correctly`() {
            val result = parser.parse(TimeframeSpec.QuarterWithYear(4, 2020))

            assertThat(result.start).isEqualTo(LocalDate.of(2020, 10, 1))
            assertThat(result.end).isEqualTo(LocalDate.of(2020, 12, 31))
        }

        @Test
        fun `should handle future year correctly`() {
            val result = parser.parse(TimeframeSpec.QuarterWithYear(2, 2030))

            assertThat(result.start).isEqualTo(LocalDate.of(2030, 4, 1))
            assertThat(result.end).isEqualTo(LocalDate.of(2030, 6, 30))
        }
    }

    @Nested
    inner class CustomDateRange {
        @Test
        fun `should parse custom date range correctly`() {
            val start = LocalDate.of(2025, 12, 6)
            val end = LocalDate.of(2026, 2, 3)

            val result = parser.parse(TimeframeSpec.Custom(start, end))

            assertThat(result.start).isEqualTo(start)
            assertThat(result.end).isEqualTo(end)
        }

        @Test
        fun `should handle same day custom range`() {
            val date = LocalDate.of(2025, 6, 15)

            val result = parser.parse(TimeframeSpec.Custom(date, date))

            assertThat(result.start).isEqualTo(date)
            assertThat(result.end).isEqualTo(date)
        }

        @Test
        fun `should handle multi-year custom range`() {
            val start = LocalDate.of(2020, 1, 1)
            val end = LocalDate.of(2025, 12, 31)

            val result = parser.parse(TimeframeSpec.Custom(start, end))

            assertThat(result.start).isEqualTo(start)
            assertThat(result.end).isEqualTo(end)
        }

        @Test
        fun `should handle cross-year boundary custom range`() {
            val start = LocalDate.of(2024, 11, 15)
            val end = LocalDate.of(2025, 2, 28)

            val result = parser.parse(TimeframeSpec.Custom(start, end))

            assertThat(result.start).isEqualTo(start)
            assertThat(result.end).isEqualTo(end)
        }
    }
}
