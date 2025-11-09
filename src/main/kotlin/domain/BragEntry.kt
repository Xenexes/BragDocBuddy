package domain

import java.time.LocalDate
import java.time.LocalDateTime

data class BragEntry(
    val timestamp: LocalDateTime,
    val content: String,
) {
    val date: LocalDate
        get() = timestamp.toLocalDate()
}
