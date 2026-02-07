package domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.Year

class TimeframeSpecTest {
    @Nested
    inner class PredefinedTimeframes {
        @ParameterizedTest
        @CsvSource(
            "today, TODAY",
            "TODAY, TODAY",
            "yesterday, YESTERDAY",
            "last-week, LAST_WEEK",
            "lastweek, LAST_WEEK",
            "last-month, LAST_MONTH",
            "last-year, LAST_YEAR",
            "q1, QUARTER_ONE",
            "Q1, QUARTER_ONE",
            "q2, QUARTER_TWO",
            "q3, QUARTER_THREE",
            "q4, QUARTER_FOUR",
        )
        fun `should parse predefined timeframes`(
            input: String,
            expected: String,
        ) {
            val result = TimeframeSpec.fromString(input)

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.Predefined>()
            assertThat((result as TimeframeSpec.Predefined).timeframe).isEqualTo(Timeframe.valueOf(expected))
        }
    }

    @Nested
    inner class QuarterWithYear {
        @ParameterizedTest
        @CsvSource(
            "q1 2025, 1, 2025",
            "Q1 2025, 1, 2025",
            "q2 2024, 2, 2024",
            "Q2 2024, 2, 2024",
            "q3 2023, 3, 2023",
            "Q3 2023, 3, 2023",
            "q4 2022, 4, 2022",
            "Q4 2022, 4, 2022",
        )
        fun `should parse quarter with year`(
            input: String,
            expectedQuarter: Int,
            expectedYear: Int,
        ) {
            val result = TimeframeSpec.fromString(input)

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.QuarterWithYear>()
            val quarterWithYear = result as TimeframeSpec.QuarterWithYear
            assertThat(quarterWithYear.quarter).isEqualTo(expectedQuarter)
            assertThat(quarterWithYear.year).isEqualTo(expectedYear)
        }

        @Test
        fun `should handle multiple spaces between quarter and year`() {
            val result = TimeframeSpec.fromString("q1  2025")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.QuarterWithYear>()
            val quarterWithYear = result as TimeframeSpec.QuarterWithYear
            assertThat(quarterWithYear.quarter).isEqualTo(1)
            assertThat(quarterWithYear.year).isEqualTo(2025)
        }

        @Test
        fun `should handle leading and trailing whitespace`() {
            val result = TimeframeSpec.fromString("  q1 2025  ")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.QuarterWithYear>()
            val quarterWithYear = result as TimeframeSpec.QuarterWithYear
            assertThat(quarterWithYear.quarter).isEqualTo(1)
            assertThat(quarterWithYear.year).isEqualTo(2025)
        }

        @Test
        fun `should create quarter with default year using factory method`() {
            val currentYear = Year.now().value
            val result = TimeframeSpec.quarter(1)

            assertThat(result).isInstanceOf<TimeframeSpec.QuarterWithYear>()
            val quarterWithYear = result as TimeframeSpec.QuarterWithYear
            assertThat(quarterWithYear.quarter).isEqualTo(1)
            assertThat(quarterWithYear.year).isEqualTo(currentYear)
        }

        @Test
        fun `should create quarter with specific year using factory method`() {
            val result = TimeframeSpec.quarter(2, 2024)

            assertThat(result).isInstanceOf<TimeframeSpec.QuarterWithYear>()
            val quarterWithYear = result as TimeframeSpec.QuarterWithYear
            assertThat(quarterWithYear.quarter).isEqualTo(2)
            assertThat(quarterWithYear.year).isEqualTo(2024)
        }
    }

    @Nested
    inner class CustomDateRange {
        @Test
        fun `should parse custom date range`() {
            val result = TimeframeSpec.fromString("06.12.2025-03.02.2026")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.Custom>()
            val custom = result as TimeframeSpec.Custom
            assertThat(custom.start).isEqualTo(LocalDate.of(2025, 12, 6))
            assertThat(custom.end).isEqualTo(LocalDate.of(2026, 2, 3))
        }

        @Test
        fun `should parse custom date range with same date`() {
            val result = TimeframeSpec.fromString("15.01.2025-15.01.2025")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.Custom>()
            val custom = result as TimeframeSpec.Custom
            assertThat(custom.start).isEqualTo(LocalDate.of(2025, 1, 15))
            assertThat(custom.end).isEqualTo(LocalDate.of(2025, 1, 15))
        }

        @Test
        fun `should parse custom date range spanning years`() {
            val result = TimeframeSpec.fromString("01.11.2024-28.02.2025")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.Custom>()
            val custom = result as TimeframeSpec.Custom
            assertThat(custom.start).isEqualTo(LocalDate.of(2024, 11, 1))
            assertThat(custom.end).isEqualTo(LocalDate.of(2025, 2, 28))
        }

        @Test
        fun `should handle leading and trailing whitespace`() {
            val result = TimeframeSpec.fromString("  06.12.2025-03.02.2026  ")

            assertThat(result).isNotNull().isInstanceOf<TimeframeSpec.Custom>()
        }

        @Test
        fun `should return null for invalid date format`() {
            assertThat(TimeframeSpec.fromString("2025-12-06-2026-02-03")).isNull()
            assertThat(TimeframeSpec.fromString("06/12/2025-03/02/2026")).isNull()
            assertThat(TimeframeSpec.fromString("06.12.25-03.02.26")).isNull()
        }

        @Test
        fun `should return null for invalid dates`() {
            assertThat(TimeframeSpec.fromString("32.12.2025-03.02.2026")).isNull()
            assertThat(TimeframeSpec.fromString("06.13.2025-03.02.2026")).isNull()
        }

        @Test
        fun `should create custom range using factory method`() {
            val start = LocalDate.of(2025, 1, 1)
            val end = LocalDate.of(2025, 12, 31)

            val result = TimeframeSpec.customRange(start, end)

            assertThat(result).isInstanceOf<TimeframeSpec.Custom>()
            val custom = result as TimeframeSpec.Custom
            assertThat(custom.start).isEqualTo(start)
            assertThat(custom.end).isEqualTo(end)
        }
    }

    @Nested
    inner class InvalidInputs {
        @ParameterizedTest
        @CsvSource(
            "invalid",
            "tomorrow",
            "next-week",
            "q5",
            "q0",
            "q1 abcd",
            "q5 2025",
        )
        fun `should return null for invalid inputs`(input: String) {
            val result = TimeframeSpec.fromString(input)

            assertThat(result).isNull()
        }

        @Test
        fun `should return null for empty string`() {
            assertThat(TimeframeSpec.fromString("")).isNull()
        }

        @Test
        fun `should return null for whitespace only`() {
            assertThat(TimeframeSpec.fromString("   ")).isNull()
        }
    }

    @Nested
    inner class FactoryMethods {
        @Test
        fun `should create today timeframe`() {
            val result = TimeframeSpec.today()

            assertThat(result).isInstanceOf<TimeframeSpec.Predefined>()
            assertThat((result as TimeframeSpec.Predefined).timeframe).isEqualTo(Timeframe.TODAY)
        }
    }

    @Nested
    inner class ValidationConstraints {
        @Test
        fun `should throw exception when creating QuarterWithYear with invalid quarter`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                TimeframeSpec.QuarterWithYear(0, 2025)
            }
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                TimeframeSpec.QuarterWithYear(5, 2025)
            }
        }

        @Test
        fun `should throw exception when creating Custom with end before start`() {
            org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
                TimeframeSpec.Custom(
                    LocalDate.of(2025, 12, 31),
                    LocalDate.of(2025, 1, 1),
                )
            }
        }
    }
}
