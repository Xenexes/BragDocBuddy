package ports

import domain.ai.dto.SummarizationResult

interface ReportWriter {
    fun saveReport(result: SummarizationResult): String
}
