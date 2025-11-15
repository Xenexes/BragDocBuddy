package infrastructure.persistence

import domain.BragEntry
import domain.DateRange
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.BragRepository
import java.io.File
import java.time.format.DateTimeFormatter

class MarkdownBragRepository(
    private val docsLocation: String,
    private val fileHandler: MarkdownBragFileHandler = MarkdownBragFileHandler(),
) : BragRepository {
    private val logger = KotlinLogging.logger {}
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private fun getBragFileForYear(year: Int): File = File(docsLocation, "brags-$year.md")

    override fun save(entry: BragEntry): Boolean {
        val year = entry.timestamp.year
        val bragFile = getBragFileForYear(year)
        val entriesByDate = fileHandler.parseAllEntries(bragFile).toMutableMap()

        val allExistingEntries = entriesByDate.values.flatten()
        if (allExistingEntries.any { it.content == entry.content }) {
            logger.debug { "Skipping duplicate entry: ${entry.content}" }
            return false
        }

        val dateKey = entry.date
        val existingEntries = entriesByDate.getOrDefault(dateKey, mutableListOf()).toMutableList()
        existingEntries.add(entry)
        entriesByDate[dateKey] = existingEntries

        val content = fileHandler.formatEntries(year, entriesByDate)
        bragFile.writeText(content)

        logger.debug { "Saved brag entry for ${entry.date.format(dateFormatter)}: ${entry.content}" }
        return true
    }

    override fun findByDateRange(range: DateRange): List<BragEntry> {
        val startYear = range.start.year
        val endYear = range.end.year
        val allEntries = mutableListOf<BragEntry>()

        for (year in startYear..endYear) {
            val bragFile = getBragFileForYear(year)
            if (!bragFile.exists()) {
                logger.debug { "Brag file does not exist: ${bragFile.absolutePath}" }
                continue
            }

            val entries = fileHandler.parseEntriesInRange(bragFile) { date -> range.contains(date) }
            allEntries.addAll(entries)
        }

        logger.debug { "Found ${allEntries.size} entries in date range ${range.start} to ${range.end}" }
        return allEntries.sortedBy { it.timestamp }
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
