package infrastructure.persistence

import domain.BragEntry
import domain.DateRange
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.BragRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MarkdownBragRepository(
    private val docsLocation: String,
) : BragRepository {
    private val logger = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val bragFile: File
        get() = File(docsLocation, "brags.md")

    override fun save(entry: BragEntry) {
        val dateStr = entry.date.format(dateFormatter)
        val timeStr = entry.timestamp.format(timeFormatter)

        val content =
            if (bragFile.exists()) {
                val existing = bragFile.readText()
                if (existing.contains("## $dateStr")) {
                    "* $timeStr ${entry.content}\n"
                } else {
                    "\n## $dateStr\n* $timeStr ${entry.content}\n"
                }
            } else {
                "# Brags\n\n## $dateStr\n* $timeStr ${entry.content}\n"
            }

        bragFile.appendText(content)
        logger.debug { "Saved brag entry for $dateStr: ${entry.content}" }
    }

    override fun findByDateRange(range: DateRange): List<BragEntry> {
        if (!bragFile.exists()) {
            logger.debug { "Brag file does not exist: ${bragFile.absolutePath}" }
            return emptyList()
        }

        val entries = mutableListOf<BragEntry>()
        val lines = bragFile.readLines()
        var currentDate: LocalDate? = null

        for (line in lines) {
            when {
                line.startsWith("## ") -> {
                    val dateStr = line.substring(3).trim()
                    currentDate =
                        try {
                            LocalDate.parse(dateStr, dateFormatter)
                        } catch (e: Exception) {
                            logger.warn { "Skipping malformed date header: '$dateStr'. Error: ${e.message}" }
                            null
                        }
                }

                line.startsWith("* ") && currentDate != null -> {
                    if (range.contains(currentDate)) {
                        val parts = line.substring(2).split(" ", limit = 2)
                        if (parts.size == 2) {
                            try {
                                val time = parts[0]
                                val content = parts[1]
                                val timestamp =
                                    LocalDateTime.parse(
                                        "${currentDate}T$time",
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                                    )
                                entries.add(BragEntry(timestamp, content))
                            } catch (e: Exception) {
                                logger.warn { "Skipping malformed entry on $currentDate: '$line'. Error: ${e.message}" }
                            }
                        } else {
                            logger.warn { "Skipping entry with invalid format on $currentDate: '$line'" }
                        }
                    }
                }
            }
        }

        logger.debug { "Found ${entries.size} entries in date range ${range.start} to ${range.end}" }
        return entries
    }

    override fun isInitialized(): Boolean {
        val docsDir = File(docsLocation)
        return docsDir.exists() && File(docsDir, ".git").exists()
    }

    override fun initialize() {
        val docsDir = File(docsLocation)

        if (!docsDir.exists()) {
            throw IllegalStateException(
                "Directory $docsLocation does not exist. " +
                    "Please create a git repository at this location first",
            )
        }

        val gitDir = File(docsDir, ".git")
        if (!gitDir.exists()) {
            throw IllegalStateException(
                "$docsLocation is not a git repository. " +
                    "Please initialize git in this directory: git init",
            )
        }

        val readmeFile = File(docsDir, "README.md")
        if (!readmeFile.exists()) {
            readmeFile.writeText(
                """
                # Bragging Document
                
                This is my bragging document where I keep track of my accomplishments.

                The core problem it solves:

                Both you and your manager forget what you've achieved over time, making performance reviews difficult and potentially costing you recognition, promotions, or raises. By regularly documenting your work, you create a reliable record that helps with performance reviews, manager transitions, and career reflection.

                When writing entries, remember to:

                * Capture everything, even small wins you think you'll remember (you won't)
                * Include "fuzzy work" like mentoring, process improvements, and code quality efforts that often go unrecognized
                * Focus on impact and results, not just activities (e.g., "reduced build time by 40%, saving team 2 hours daily" vs "improved build process").
                * Document the "why" behind your work to show the bigger picture
                * Record what you learned and skills you're developing

                Don't oversell or undersell - just make your work sound exactly as good as it is.
                
                Inspired by [Julia Evans' blog post on brag documents](https://jvns.ca/blog/brag-documents/)
                """.trimIndent(),
            )
            logger.info { "Initialized bragging document at $docsLocation" }
        }
    }
}
