package domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TimeframeTest {
    @Test
    fun `should have all expected timeframe values`() {
        val timeframes = Timeframe.entries.toTypedArray()

        assertThat(timeframes.size).isEqualTo(9)
        assertThat(timeframes).isEqualTo(
            arrayOf(
                Timeframe.TODAY,
                Timeframe.YESTERDAY,
                Timeframe.LAST_WEEK,
                Timeframe.LAST_MONTH,
                Timeframe.LAST_YEAR,
                Timeframe.QUARTER_ONE,
                Timeframe.QUARTER_TWO,
                Timeframe.QUARTER_THREE,
                Timeframe.QUARTER_FOUR,
            ),
        )
    }

    @ParameterizedTest
    @CsvSource(
        "today, TODAY",
        "TODAY, TODAY",
        "ToDay, TODAY",
        "yesterday, YESTERDAY",
        "YESTERDAY, YESTERDAY",
        "YesterDay, YESTERDAY",
        "last-week, LAST_WEEK",
        "lastweek, LAST_WEEK",
        "LAST-WEEK, LAST_WEEK",
        "LASTWEEK, LAST_WEEK",
        "last-month, LAST_MONTH",
        "lastmonth, LAST_MONTH",
        "LAST-MONTH, LAST_MONTH",
        "LASTMONTH, LAST_MONTH",
        "last-year, LAST_YEAR",
        "lastyear, LAST_YEAR",
        "LAST-YEAR, LAST_YEAR",
        "LASTYEAR, LAST_YEAR",
        "quarter-one, QUARTER_ONE",
        "quarterone, QUARTER_ONE",
        "q1, QUARTER_ONE",
        "Q1, QUARTER_ONE",
        "quarter-two, QUARTER_TWO",
        "quartertwo, QUARTER_TWO",
        "q2, QUARTER_TWO",
        "Q2, QUARTER_TWO",
        "quarter-three, QUARTER_THREE",
        "quarterthree, QUARTER_THREE",
        "q3, QUARTER_THREE",
        "Q3, QUARTER_THREE",
        "quarter-four, QUARTER_FOUR",
        "quarterfour, QUARTER_FOUR",
        "q4, QUARTER_FOUR",
        "Q4, QUARTER_FOUR",
    )
    fun `should parse valid timeframe strings case insensitively`(
        input: String,
        expected: String,
    ) {
        val result = Timeframe.fromString(input)

        assertThat(result).isEqualTo(Timeframe.valueOf(expected))
    }

    @ParameterizedTest
    @CsvSource(
        "invalid",
        "tomorrow",
        "next-week",
        "last_week",
        "last week",
        "week",
        "month",
        "year",
        "''",
        "null",
    )
    fun `should return null for invalid timeframe strings`(input: String) {
        val result = Timeframe.fromString(input)

        assertThat(result).isNull()
    }

    @Test
    fun `should return null for empty string`() {
        val result = Timeframe.fromString("")

        assertThat(result).isNull()
    }

    @Test
    fun `should handle whitespace in input`() {
        val result1 = Timeframe.fromString(" today ")
        val result2 = Timeframe.fromString("\tlast-week\n")

        assertThat(result1).isNull() // Whitespace is not trimmed
        assertThat(result2).isNull() // Whitespace is not trimmed
    }

    @Test
    fun `should parse all valid string variations`() {
        val validInputs =
            mapOf(
                "today" to Timeframe.TODAY,
                "yesterday" to Timeframe.YESTERDAY,
                "last-week" to Timeframe.LAST_WEEK,
                "lastweek" to Timeframe.LAST_WEEK,
                "last-month" to Timeframe.LAST_MONTH,
                "lastmonth" to Timeframe.LAST_MONTH,
                "last-year" to Timeframe.LAST_YEAR,
                "lastyear" to Timeframe.LAST_YEAR,
                "quarter-one" to Timeframe.QUARTER_ONE,
                "quarterone" to Timeframe.QUARTER_ONE,
                "q1" to Timeframe.QUARTER_ONE,
                "quarter-two" to Timeframe.QUARTER_TWO,
                "quartertwo" to Timeframe.QUARTER_TWO,
                "q2" to Timeframe.QUARTER_TWO,
                "quarter-three" to Timeframe.QUARTER_THREE,
                "quarterthree" to Timeframe.QUARTER_THREE,
                "q3" to Timeframe.QUARTER_THREE,
                "quarter-four" to Timeframe.QUARTER_FOUR,
                "quarterfour" to Timeframe.QUARTER_FOUR,
                "q4" to Timeframe.QUARTER_FOUR,
            )

        validInputs.forEach { (input, expected) ->
            assertThat(Timeframe.fromString(input)).isEqualTo(expected)
        }
    }
}
