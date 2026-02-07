package ports

import domain.DateRange
import domain.TimeframeSpec

fun interface TimeframeParser {
    fun parse(timeframeSpec: TimeframeSpec): DateRange
}
