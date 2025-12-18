package api.cli.commands

import domain.Timeframe
import domain.ai.NoEntriesFoundException
import domain.ai.SummarizationException
import domain.ai.dto.TemplateType
import usecases.SummarizeBragsUseCase

/**
 * Command to summarize brag entries using AI and prompt templates.
 *
 * This command is responsible ONLY for presentation logic (CLI output).
 * All business logic (AI summarization) and infrastructure (file I/O)
 * are handled by the use case and its dependencies.
 *
 * Usage: brag-doc summarize <timeframe> <template-type>
 *
 * Examples:
 * - brag-doc summarize q4 bullet
 * - brag-doc summarize last-month quarterly-report
 * - brag-doc summarize last-year performance-review
 */
class SummarizeCommand(
    private val summarizeUseCase: SummarizeBragsUseCase,
    private val timeframe: Timeframe,
    private val templateType: TemplateType,
) : Command {
    override fun execute() {
        println(
            "Summarizing ${
                timeframe.name.lowercase().replace(
                    '_',
                    '-',
                )
            } entries using ${templateType.name.lowercase().replace('_', '-')} template...",
        )
        println()

        try {
            val result = summarizeUseCase.execute(timeframe, templateType)

            println("=" * 80)
            println("Summary Generated Successfully")
            println("=" * 80)
            println()
            println("Timeframe: ${result.timeframe.name.lowercase().replace('_', '-')}")
            println("Template: ${result.templateType.name.lowercase().replace('_', '-')}")
            println("Entries processed: ${result.entriesCount}")

            result.savedReportPath?.let {
                println("Saved to: $it")
            }

            println()
            println("-" * 80)
            println(result.summary)
            println("-" * 80)
            println()
            println("Tip: You can customize the template by running 'brag-doc templates export'")
            println("     and editing the file in ${'$'}BRAG_DOC/.templates/${templateType.fileName}")
        } catch (e: NoEntriesFoundException) {
            println("Error: ${e.message}")
            println()
            println("Add some brag entries first:")
            println("  brag-doc add")
            println()
            println("Or sync from external sources:")
            println("  brag-doc sync-prs ${timeframe.name.lowercase().replace('_', '-')}")
            println("  brag-doc sync-jira ${timeframe.name.lowercase().replace('_', '-')}")
        } catch (e: SummarizationException) {
            println("Error: Failed to generate summary")
            println("Details: ${e.message}")
            println()
            println("Make sure Ollama is running:")
            println("  Visit: http://localhost:11434")
            println()
            println("Check your configuration:")
            println("  BRAG_DOC_OLLAMA_URL: ${System.getenv("BRAG_DOC_OLLAMA_URL") ?: "http://localhost:11434 (default)"}")
            println("  BRAG_DOC_OLLAMA_MODEL: ${System.getenv("BRAG_DOC_OLLAMA_MODEL") ?: "llama3.2 (default)"}")
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
            e.printStackTrace()
        }
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)
