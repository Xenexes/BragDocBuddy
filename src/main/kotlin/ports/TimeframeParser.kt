package ports

import domain.DateRange
import domain.Timeframe

fun interface TimeframeParser {
    fun parse(timeframe: Timeframe): DateRange
}
