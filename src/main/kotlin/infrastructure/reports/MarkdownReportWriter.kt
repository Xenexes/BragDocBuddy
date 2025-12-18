package infrastructure.reports

import ApplicationConfiguration
import domain.ai.dto.SummarizationResult
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.ReportWriter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MarkdownReportWriter(
    private val appConfiguration: ApplicationConfiguration,
) : ReportWriter {
    private val logger = KotlinLogging.logger {}

    override fun saveReport(result: SummarizationResult): String {
        val reportsDir = File(appConfiguration.docsLocation, REPORTS_DIRECTORY)
        ensureDirectoryExists(reportsDir)

        val fileName = generateFileName(result)
        val reportFile = File(reportsDir, fileName)

        val content = generateMarkdownContent(result)
        reportFile.writeText(content)

        logger.info { "Saved report: ${reportFile.absolutePath}" }
        return reportFile.absolutePath
    }

    private fun ensureDirectoryExists(directory: File) {
        if (!directory.exists()) {
            directory.mkdirs()
            logger.info { "Created reports directory: ${directory.absolutePath}" }
        }
    }

    private fun generateFileName(result: SummarizationResult): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"))
        val timeframeName =
            result.timeframe.name
                .lowercase()
                .replace('_', '-')
        val templateName =
            result.templateType.name
                .lowercase()
                .replace('_', '-')

        return "${timeframeName}_${templateName}_$timestamp.md"
    }

    private fun generateMarkdownContent(result: SummarizationResult): String =
        buildString {
            appendLine("# Brag Document Summary")
            appendLine()
            appendLine("**Generated:** ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
            appendLine("**Timeframe:** ${result.timeframe.name.lowercase().replace('_', '-')}")
            appendLine("**Template:** ${result.templateType.name.lowercase().replace('_', '-')}")
            appendLine("**Entries Processed:** ${result.entriesCount}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine(result.summary)
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("*Generated with BragDocBuddy - https://github.com/Xenexes/BragDocBuddy*")
        }

    companion object {
        const val REPORTS_DIRECTORY = ".reports"
    }
}
