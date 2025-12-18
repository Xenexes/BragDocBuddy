package domain.ai.dto

import domain.Timeframe

data class SummarizationResult(
    val summary: String,
    val entriesCount: Int,
    val timeframe: Timeframe,
    val templateType: TemplateType,
    val savedReportPath: String? = null,
)
