package usecases

import domain.BragEntry
import domain.TimeframeSpec
import ports.BragRepository
import ports.TimeframeParser

class GetBragsUseCase(
    private val repository: BragRepository,
    private val timeframeParser: TimeframeParser,
) {
    fun getBrags(timeframeSpec: TimeframeSpec): Map<String, List<BragEntry>> {
        if (!repository.isInitialized()) {
            throw IllegalStateException(
                "Repository not initialized. Run 'brag init' first",
            )
        }

        val dateRange = timeframeParser.parse(timeframeSpec)

        val entries = repository.findByDateRange(dateRange)

        return entries.groupBy { it.date.toString() }
    }
}
