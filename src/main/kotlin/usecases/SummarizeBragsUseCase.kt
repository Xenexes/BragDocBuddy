package usecases

import domain.BragEntry
import domain.Timeframe
import domain.ai.NoEntriesFoundException
import domain.ai.PromptTemplate
import domain.ai.SummarizationException
import domain.ai.dto.SummarizationResult
import domain.ai.dto.TemplateType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import ports.AIClient
import ports.ReportWriter
import ports.TemplateService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SummarizeBragsUseCase(
    private val getBragsUseCase: GetBragsUseCase,
    private val templateService: TemplateService,
    private val aiClient: AIClient,
    private val reportWriter: ReportWriter
) {
    private val logger = KotlinLogging.logger {}

    fun execute(
        timeframe: Timeframe,
        templateType: TemplateType,
    ): SummarizationResult =
        runBlocking {
            logger.info { "Starting summarization for timeframe: ${timeframe.name}, template: ${templateType.name}" }

            val entries = getBragEntriesForTimeframe(timeframe)

            logger.info { "Found ${entries.size} brag entries for ${timeframe.name}" }

            val template = templateService.loadTemplate(templateType)

            logger.debug { "Loaded template: ${templateType.fileName}" }

            val formattedEntries = formatEntriesForPrompt(entries)

            val filledPrompt =
                fillTemplatePlaceholders(timeframe, template, entries, formattedEntries)

            logger.debug { "Filled prompt template with ${entries.size} entries" }

            val result = getSummaryFromLLM(filledPrompt, entries, timeframe, templateType)

            val savedPath = reportWriter.saveReport(result)
            logger.info { "Saved report to: $savedPath" }

            return@runBlocking result.copy(savedReportPath = savedPath)
        }

    private suspend fun getSummaryFromLLM(
        filledPrompt: String,
        entries: List<BragEntry>,
        timeframe: Timeframe,
        templateType: TemplateType,
    ): SummarizationResult {
        try {
            val summary = aiClient.generate(userPrompt = filledPrompt)
            logger.info { "Successfully generated summary (${summary.length} characters)" }

            return SummarizationResult(
                summary = summary,
                entriesCount = entries.size,
                timeframe = timeframe,
                templateType = templateType,
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate summary" }
            throw SummarizationException("Failed to generate summary: ${e.message}", e)
        }
    }

    private fun fillTemplatePlaceholders(
        timeframe: Timeframe,
        template: PromptTemplate,
        entries: List<BragEntry>,
        formattedEntries: String,
    ): String {
        val timeframeDescription = getTimeframeDescription(timeframe)
        val filledPrompt =
            template.fillPlaceholders(
                mapOf(
                    "timeframe" to timeframeDescription,
                    "count" to entries.size.toString(),
                    "entries" to formattedEntries,
                ),
            )
        return filledPrompt
    }

    private fun getBragEntriesForTimeframe(timeframe: Timeframe): List<BragEntry> {
        val entries = getBragsUseCase.getBrags(timeframe).values.flatten()
        if (entries.isEmpty()) {
            throw NoEntriesFoundException(
                "No brag entries found for timeframe: ${timeframe.name}. " +
                    "Add some entries first using 'brag-doc add'.",
            )
        }
        return entries
    }

    private fun formatEntriesForPrompt(entries: List<BragEntry>): String =
        entries
            .mapIndexed { index, entry ->
                "${index + 1}. ${entry.content}\n   Date: ${entry.date.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
            }.joinToString("\n\n")

    private fun getTimeframeDescription(timeframe: Timeframe): String {
        val currentYear = LocalDate.now().year
        return when (timeframe) {
            Timeframe.TODAY -> "today"
            Timeframe.YESTERDAY -> "yesterday"
            Timeframe.LAST_WEEK -> "last week"
            Timeframe.LAST_MONTH -> "last month"
            Timeframe.LAST_YEAR -> "last year ($currentYear)"
            Timeframe.QUARTER_ONE -> "Q1 $currentYear"
            Timeframe.QUARTER_TWO -> "Q2 $currentYear"
            Timeframe.QUARTER_THREE -> "Q3 $currentYear"
            Timeframe.QUARTER_FOUR -> "Q4 $currentYear"
        }
    }
}
