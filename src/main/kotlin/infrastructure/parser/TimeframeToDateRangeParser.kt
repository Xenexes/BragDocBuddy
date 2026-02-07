package infrastructure.parser

import domain.DateRange
import domain.Timeframe
import domain.TimeframeSpec
import ports.TimeframeParser
import java.time.LocalDate
import java.time.Year

class TimeframeToDateRangeParser : TimeframeParser {
    override fun parse(timeframeSpec: TimeframeSpec): DateRange =
        when (timeframeSpec) {
            is TimeframeSpec.Predefined -> parsePredefined(timeframeSpec.timeframe)
            is TimeframeSpec.QuarterWithYear -> getQuarterDateRange(timeframeSpec.year, timeframeSpec.quarter)
            is TimeframeSpec.Custom -> DateRange(timeframeSpec.start, timeframeSpec.end)
        }

    private fun parsePredefined(timeframe: Timeframe): DateRange {
        val today = LocalDate.now()

        return when (timeframe) {
            Timeframe.TODAY -> DateRange(today, today)
            Timeframe.YESTERDAY -> {
                val yesterday = today.minusDays(1)
                DateRange(yesterday, yesterday)
            }

            Timeframe.LAST_WEEK -> DateRange(today.minusWeeks(1), today)
            Timeframe.LAST_MONTH -> DateRange(today.minusMonths(1), today)
            Timeframe.LAST_YEAR -> DateRange(today.minusYears(1), today)
            Timeframe.QUARTER_ONE -> getQuarterDateRange(Year.now().value, 1)
            Timeframe.QUARTER_TWO -> getQuarterDateRange(Year.now().value, 2)
            Timeframe.QUARTER_THREE -> getQuarterDateRange(Year.now().value, 3)
            Timeframe.QUARTER_FOUR -> getQuarterDateRange(Year.now().value, 4)
        }
    }

    private fun getQuarterDateRange(
        year: Int,
        quarter: Int,
    ): DateRange {
        require(quarter in 1..4) { "Quarter must be between 1 and 4" }

        val startMonth = (quarter - 1) * 3 + 1
        val endMonth = quarter * 3

        val startDate = LocalDate.of(year, startMonth, 1)
        val endDate =
            LocalDate
                .of(year, endMonth, 1)
                .plusMonths(1)
                .minusDays(1)

        return DateRange(startDate, endDate)
    }
}
