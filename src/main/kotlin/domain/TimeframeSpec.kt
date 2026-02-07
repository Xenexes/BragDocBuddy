package domain

import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Represents a timeframe specification that can be used to filter brag entries.
 *
 * Supports three types of timeframe specifications:
 * - [Predefined]: Standard timeframes like today, yesterday, last-week, q1, etc.
 * - [QuarterWithYear]: A specific quarter in a specific year (e.g., "q1 2025")
 * - [Custom]: A custom date range with explicit start and end dates
 *
 * @see Timeframe for the list of predefined timeframes
 */
sealed interface TimeframeSpec {
    /**
     * A predefined timeframe from the [Timeframe] enum.
     *
     * @property timeframe The predefined timeframe value
     */
    data class Predefined(
        val timeframe: Timeframe,
    ) : TimeframeSpec

    /**
     * A quarter specification for a specific year.
     *
     * @property quarter The quarter number (1-4)
     * @property year The year (e.g., 2025)
     * @throws IllegalArgumentException if quarter is not between 1 and 4
     */
    data class QuarterWithYear(
        val quarter: Int,
        val year: Int,
    ) : TimeframeSpec {
        init {
            require(quarter in 1..4) { "Quarter must be between 1 and 4" }
        }
    }

    /**
     * A custom date range with explicit start and end dates.
     *
     * @property start The start date of the range (inclusive)
     * @property end The end date of the range (inclusive)
     * @throws IllegalArgumentException if start date is after end date
     */
    data class Custom(
        val start: LocalDate,
        val end: LocalDate,
    ) : TimeframeSpec {
        init {
            require(!start.isAfter(end)) { "Start date must be before or equal to end date" }
        }
    }

    companion object {
        private val CUSTOM_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val CUSTOM_RANGE_PATTERN = Regex("""(\d{2}\.\d{2}\.\d{4})-(\d{2}\.\d{2}\.\d{4})""")
        private val QUARTER_WITH_YEAR_PATTERN = Regex("""q([1-4])\s+(\d{4})""", RegexOption.IGNORE_CASE)

        /**
         * Parses a string into a [TimeframeSpec].
         *
         * Supported formats:
         * - Predefined timeframes: "today", "yesterday", "last-week", "q1", etc.
         * - Quarter with year: "q1 2025", "Q2 2024"
         * - Custom date range: "DD.MM.YYYY-DD.MM.YYYY" (e.g., "06.12.2025-03.02.2026")
         *
         * @param value The string to parse
         * @return The parsed [TimeframeSpec], or null if the string is invalid
         */
        fun fromString(value: String): TimeframeSpec? {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return null

            // Try predefined timeframe first
            Timeframe.fromString(trimmed)?.let { return Predefined(it) }

            // Try quarter with year format (e.g., "q1 2025")
            QUARTER_WITH_YEAR_PATTERN.matchEntire(trimmed)?.let { match ->
                val quarter = match.groupValues[1].toInt()
                val year = match.groupValues[2].toInt()
                return QuarterWithYear(quarter, year)
            }

            // Try custom date range format (e.g., "06.12.2025-03.02.2026")
            CUSTOM_RANGE_PATTERN.matchEntire(trimmed)?.let { match ->
                return try {
                    val startDate = LocalDate.parse(match.groupValues[1], CUSTOM_DATE_FORMAT)
                    val endDate = LocalDate.parse(match.groupValues[2], CUSTOM_DATE_FORMAT)
                    Custom(startDate, endDate)
                } catch (e: DateTimeParseException) {
                    null
                }
            }

            return null
        }

        /**
         * Creates a [TimeframeSpec] for today.
         *
         * @return A [Predefined] timeframe for today
         */
        fun today(): TimeframeSpec = Predefined(Timeframe.TODAY)

        /**
         * Creates a [TimeframeSpec] for a specific quarter.
         *
         * @param quarter The quarter number (1-4)
         * @param year The year (defaults to current year)
         * @return A [QuarterWithYear] timeframe
         * @throws IllegalArgumentException if quarter is not between 1 and 4
         */
        fun quarter(
            quarter: Int,
            year: Int = Year.now().value,
        ): TimeframeSpec = QuarterWithYear(quarter, year)

        /**
         * Creates a [TimeframeSpec] for a custom date range.
         *
         * @param start The start date of the range (inclusive)
         * @param end The end date of the range (inclusive)
         * @return A [Custom] timeframe
         * @throws IllegalArgumentException if start date is after end date
         */
        fun customRange(
            start: LocalDate,
            end: LocalDate,
        ): TimeframeSpec = Custom(start, end)
    }
}
