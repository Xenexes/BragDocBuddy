package ports

import domain.BragEntry
import domain.DateRange

interface BragRepository {
    fun save(entry: BragEntry): Boolean

    fun findByDateRange(range: DateRange): List<BragEntry> // Changed from BragEntry

    fun isInitialized(): Boolean

    fun initialize()
}
