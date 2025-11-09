package usecases

import domain.BragEntry
import domain.Timeframe
import ports.BragRepository
import ports.TimeframeParser

class GetBragsUseCase(
    private val repository: BragRepository,
    private val timeframeParser: TimeframeParser,
) {
    fun getBrags(timeframe: Timeframe): Map<String, List<BragEntry>> {
        if (!repository.isInitialized()) {
            throw IllegalStateException(
                "Repository not initialized. Run 'brag init' first",
            )
        }

        val dateRange = timeframeParser.parse(timeframe)

        val entries = repository.findByDateRange(dateRange)

        return entries.groupBy { it.date.toString() }
    }
}
