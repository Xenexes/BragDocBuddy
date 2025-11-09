package domain

import java.time.LocalDate

data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(!start.isAfter(end)) { "Start date must be before or equal to end date" }
    }

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
}
