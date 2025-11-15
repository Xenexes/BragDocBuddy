package infrastructure.persistence

import domain.BragEntry
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MarkdownBragFileHandler {
    private val logger = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun parseAllEntries(bragFile: File): Map<LocalDate, List<BragEntry>> {
        if (!bragFile.exists()) {
            return emptyMap()
        }

        return parseEntries(bragFile.readLines()) { true }
    }

    fun parseEntriesInRange(
        bragFile: File,
        dateFilter: (LocalDate) -> Boolean,
    ): List<BragEntry> {
        if (!bragFile.exists()) {
            return emptyList()
        }

        val entriesByDate = parseEntries(bragFile.readLines(), dateFilter)
        return entriesByDate.values.flatten()
    }

    private fun parseEntries(
        lines: List<String>,
        dateFilter: (LocalDate) -> Boolean,
    ): Map<LocalDate, List<BragEntry>> {
        val entriesByDate = mutableMapOf<LocalDate, MutableList<BragEntry>>()
        var currentDate: LocalDate? = null

        for (line in lines) {
            when {
                line.startsWith("## ") -> {
                    currentDate = parseDateHeader(line)
                }

                line.startsWith("* ") && currentDate != null && dateFilter(currentDate) -> {
                    parseEntry(line, currentDate)?.let { entry ->
                        val entries = entriesByDate.getOrDefault(currentDate, mutableListOf())
                        entries.add(entry)
                        entriesByDate[currentDate] = entries
                    }
                }
            }
        }

        return entriesByDate
    }

    private fun parseDateHeader(line: String): LocalDate? {
        val dateStr = line.substring(3).trim()
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: Exception) {
            logger.warn { "Skipping malformed date header: '$dateStr'. Error: ${e.message}" }
            null
        }
    }

    private fun parseEntry(
        line: String,
        date: LocalDate,
    ): BragEntry? {
        val parts = line.substring(2).split(" ", limit = 2)
        if (parts.size != 2) {
            logger.warn { "Skipping entry with invalid format on $date: '$line'" }
            return null
        }

        return try {
            val time = parts[0]
            val content = parts[1]
            val timestamp =
                LocalDateTime.parse(
                    "${date}T$time",
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                )
            BragEntry(timestamp, content)
        } catch (e: Exception) {
            logger.warn { "Skipping malformed entry on $date: '$line'. Error: ${e.message}" }
            null
        }
    }

    fun formatEntries(
        year: Int,
        entriesByDate: Map<LocalDate, List<BragEntry>>,
    ): String =
        buildString {
            append("# Brags $year\n\n")

            entriesByDate.keys.sorted().forEach { date ->
                append("## ${date.format(dateFormatter)}\n")

                val sortedEntries = entriesByDate[date]!!.sortedBy { it.timestamp }
                sortedEntries.forEach { entry ->
                    val timeStr = entry.timestamp.format(timeFormatter)
                    append("* $timeStr ${entry.content}\n")
                }
                append("\n")
            }
        }
}
