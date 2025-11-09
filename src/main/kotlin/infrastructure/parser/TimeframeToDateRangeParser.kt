package infrastructure.parser

import domain.DateRange
import domain.Timeframe
import ports.TimeframeParser
import java.time.LocalDate

class TimeframeToDateRangeParser : TimeframeParser {
    override fun parse(timeframe: Timeframe): DateRange {
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
        }
    }
}
