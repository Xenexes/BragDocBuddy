package infrastructure.persistence

import domain.BragEntry
import domain.DateRange
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownBragRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var repository: MarkdownBragRepository

    @BeforeEach
    fun setup() {
        repository = MarkdownBragRepository(tempDir.toString())
    }

    private fun getBragFileForYear(year: Int): File = File(tempDir.toFile(), "brags-$year.md")

    @Test
    fun `should insert entries in chronological order`() {
        // Given - Add entries out of order
        val entry1 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "First entry",
            )
        val entry2 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 15, 0, 0),
                content = "Third entry",
            )
        val entry3 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 12, 0, 0),
                content = "Second entry",
            )

        repository.save(entry1)
        repository.save(entry2)
        repository.save(entry3)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        assertTrue(lines.contains("# Brags 2025"))
        assertTrue(lines.contains("## 2025-11-01"))

        val firstEntryLine = lines.indexOfFirst { it.contains("First entry") }
        val secondEntryLine = lines.indexOfFirst { it.contains("Second entry") }
        val thirdEntryLine = lines.indexOfFirst { it.contains("Third entry") }

        assertTrue(firstEntryLine < secondEntryLine)
        assertTrue(secondEntryLine < thirdEntryLine)
    }

    @Test
    fun `should create date sections for gap days`() {
        val entry1 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "Entry on Nov 1",
            )
        val entry2 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 5, 14, 0, 0),
                content = "Entry on Nov 5",
            )
        val entry3 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 3, 12, 0, 0),
                content = "Entry on Nov 3",
            )

        repository.save(entry1)
        repository.save(entry2)
        repository.save(entry3)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        assertTrue(lines.contains("## 2025-11-01"))
        assertTrue(lines.contains("## 2025-11-03"))
        assertTrue(lines.contains("## 2025-11-05"))

        val nov1Index = lines.indexOfFirst { it == "## 2025-11-01" }
        val nov3Index = lines.indexOfFirst { it == "## 2025-11-03" }
        val nov5Index = lines.indexOfFirst { it == "## 2025-11-05" }

        assertTrue(nov1Index < nov3Index)
        assertTrue(nov3Index < nov5Index)
    }

    @Test
    fun `should insert PR between existing manual entries based on timestamp`() {
        val manualEntry1 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "Manual entry at 10:00",
            )
        val manualEntry2 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 15, 0, 0),
                content = "Manual entry at 15:00",
            )

        repository.save(manualEntry1)
        repository.save(manualEntry2)

        val prEntry =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 12, 0, 0),
                content = "[PR #123] Merged PR - https://github.com/org/repo/pull/123",
            )
        repository.save(prEntry)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        val entry10Index = lines.indexOfFirst { it.contains("10:00") }
        val entry12Index = lines.indexOfFirst { it.contains("12:00") }
        val entry15Index = lines.indexOfFirst { it.contains("15:00") }

        assertTrue(entry10Index < entry12Index)
        assertTrue(entry12Index < entry15Index)
        assertTrue(lines[entry12Index].contains("[PR #123]"))
    }

    @Test
    fun `should retrieve entries by date range`() {
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 1, 10, 0), "Entry 1"))
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 5, 10, 0), "Entry 2"))
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 10, 10, 0), "Entry 3"))

        val dateRange = DateRange(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 5))
        val entries = repository.findByDateRange(dateRange)

        assertEquals(2, entries.size)
        assertTrue(entries.any { it.content == "Entry 1" })
        assertTrue(entries.any { it.content == "Entry 2" })
    }

    @Test
    fun `should handle empty file gracefully`() {
        val entry =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "First entry ever",
            )
        repository.save(entry)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        assertTrue(content.startsWith("# Brags 2025\n\n"))
        assertTrue(content.contains("## 2025-11-01"))
        assertTrue(content.contains("* 10:00:00 First entry ever"))
    }

    @Test
    fun `should create separate files for different years`() {
        val entry2024 =
            BragEntry(
                timestamp = LocalDateTime.of(2024, 12, 15, 10, 0, 0),
                content = "Entry from 2024",
            )
        val entry2025 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 1, 10, 14, 0, 0),
                content = "Entry from 2025",
            )

        repository.save(entry2024)
        repository.save(entry2025)

        val bragFile2024 = getBragFileForYear(2024)
        val bragFile2025 = getBragFileForYear(2025)

        assertTrue(bragFile2024.exists())
        assertTrue(bragFile2025.exists())

        val content2024 = bragFile2024.readText()
        val content2025 = bragFile2025.readText()

        assertTrue(content2024.contains("# Brags 2024"))
        assertTrue(content2024.contains("Entry from 2024"))
        assertTrue(content2025.contains("# Brags 2025"))
        assertTrue(content2025.contains("Entry from 2025"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("crossYearDateRangeScenarios")
    fun `should handle cross-year date range queries correctly`(
        @Suppress("UNUSED_PARAMETER") testName: String,
        entriesToCreate: List<BragEntry>,
        queryRange: DateRange,
        expectedCount: Int,
        expectedIncluded: List<String>,
        expectedExcluded: List<String> = emptyList(),
    ) {
        entriesToCreate.forEach { repository.save(it) }

        val entries = repository.findByDateRange(queryRange)

        assertEquals(expectedCount, entries.size, "Expected $expectedCount entries but got ${entries.size}")

        expectedIncluded.forEach { expectedContent ->
            assertTrue(
                entries.any { it.content == expectedContent },
                "Expected to find entry: '$expectedContent'",
            )
        }

        expectedExcluded.forEach { excludedContent ->
            assertTrue(
                entries.none { it.content == excludedContent },
                "Expected to NOT find entry: '$excludedContent'",
            )
        }
    }

    @Test
    fun `should retrieve entries from entire previous year when querying last-year in January with chronological order`() {
        repository.save(BragEntry(LocalDateTime.of(2024, 1, 15, 10, 0), "Jan 2024 entry"))
        repository.save(BragEntry(LocalDateTime.of(2024, 6, 15, 10, 0), "Jun 2024 entry"))
        repository.save(BragEntry(LocalDateTime.of(2024, 12, 20, 10, 0), "Dec 2024 entry"))
        repository.save(BragEntry(LocalDateTime.of(2025, 1, 5, 10, 0), "Jan 2025 entry"))
        repository.save(BragEntry(LocalDateTime.of(2025, 1, 15, 10, 0), "Jan 15 2025 entry"))

        // When - Query for last year from Jan 15, 2025 (Jan 15, 2024 to Jan 15, 2025)
        val dateRange = DateRange(LocalDate.of(2024, 1, 15), LocalDate.of(2025, 1, 15))
        val entries = repository.findByDateRange(dateRange)

        // Then - Verify entries are sorted chronologically
        assertEquals(5, entries.size)
        assertEquals("Jan 2024 entry", entries[0].content)
        assertEquals("Jun 2024 entry", entries[1].content)
        assertEquals("Dec 2024 entry", entries[2].content)
        assertEquals("Jan 2025 entry", entries[3].content)
        assertEquals("Jan 15 2025 entry", entries[4].content)
    }

    @Test
    fun `should handle date range spanning three years and create all year files`() {
        repository.save(BragEntry(LocalDateTime.of(2023, 11, 15, 10, 0), "Nov 2023 entry"))
        repository.save(BragEntry(LocalDateTime.of(2024, 6, 15, 10, 0), "Jun 2024 entry"))
        repository.save(BragEntry(LocalDateTime.of(2025, 2, 15, 10, 0), "Feb 2025 entry"))

        // When - Query spanning all three years
        val dateRange = DateRange(LocalDate.of(2023, 11, 1), LocalDate.of(2025, 2, 28))
        val entries = repository.findByDateRange(dateRange)

        assertEquals(3, entries.size)

        assertTrue(getBragFileForYear(2023).exists())
        assertTrue(getBragFileForYear(2024).exists())
        assertTrue(getBragFileForYear(2025).exists())
    }

    @Test
    fun `should not save duplicate entries with same content`() {
        val prEntry =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 12, 0, 0),
                content = "[PR #123] Fixed critical bug - https://github.com/org/repo/pull/123",
            )

        repository.save(prEntry)
        repository.save(prEntry)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        val prLines = lines.filter { it.contains("[PR #123]") }
        assertEquals(1, prLines.size, "Expected only one PR entry, but found ${prLines.size}")
    }

    @Test
    fun `should not save duplicate PR when content matches existing entry`() {
        val existingEntry =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "[PR #456] Updated documentation - https://github.com/org/repo/pull/456",
            )
        repository.save(existingEntry)

        val duplicateEntry =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 14, 0, 0),
                content = "[PR #456] Updated documentation - https://github.com/org/repo/pull/456",
            )
        repository.save(duplicateEntry)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        val prLines = lines.filter { it.contains("[PR #456]") }
        assertEquals(1, prLines.size, "Expected only one PR entry, but found ${prLines.size}")
        assertTrue(lines.any { it.contains("10:00:00") && it.contains("[PR #456]") })
    }

    @Test
    fun `should save entries with different content even if they look similar`() {
        val pr1 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 10, 0, 0),
                content = "[PR #100] Feature A - https://github.com/org/repo/pull/100",
            )
        val pr2 =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 11, 0, 0),
                content = "[PR #101] Feature A - https://github.com/org/repo/pull/101",
            )

        repository.save(pr1)
        repository.save(pr2)

        val bragFile = getBragFileForYear(2025)
        val content = bragFile.readText()
        val lines = content.lines()

        val pr100Lines = lines.filter { it.contains("[PR #100]") }
        val pr101Lines = lines.filter { it.contains("[PR #101]") }

        assertEquals(1, pr100Lines.size)
        assertEquals(1, pr101Lines.size)
    }

    @Test
    fun `should detect duplicates among multiple existing entries`() {
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 1, 9, 0, 0), "Manual entry 1"))
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 1, 10, 0, 0), "[PR #200] Original PR - https://github.com/org/repo/pull/200"))
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 1, 11, 0, 0), "Manual entry 2"))
        repository.save(BragEntry(LocalDateTime.of(2025, 11, 1, 12, 0, 0), "[PR #201] Another PR - https://github.com/org/repo/pull/201"))

        val duplicatePr =
            BragEntry(
                timestamp = LocalDateTime.of(2025, 11, 1, 13, 0, 0),
                content = "[PR #200] Original PR - https://github.com/org/repo/pull/200",
            )
        repository.save(duplicatePr)

        val dateRange = DateRange(LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 1))
        val entries = repository.findByDateRange(dateRange)

        assertEquals(4, entries.size)
        assertEquals(1, entries.count { it.content.contains("[PR #200]") })
    }

    companion object {
        @JvmStatic
        fun crossYearDateRangeScenarios(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "Two-year span (Dec 2024 - Jan 2025)",
                    listOf(
                        BragEntry(LocalDateTime.of(2024, 12, 15, 10, 0), "Entry from Dec 2024"),
                        BragEntry(LocalDateTime.of(2025, 1, 10, 10, 0), "Entry from Jan 2025"),
                        BragEntry(LocalDateTime.of(2025, 2, 5, 10, 0), "Entry from Feb 2025"),
                    ),
                    DateRange(LocalDate.of(2024, 12, 1), LocalDate.of(2025, 1, 31)),
                    2,
                    listOf("Entry from Dec 2024", "Entry from Jan 2025"),
                    listOf("Entry from Feb 2025"),
                ),
                Arguments.of(
                    "Last 4 weeks in early January",
                    listOf(
                        BragEntry(LocalDateTime.of(2024, 12, 10, 10, 0), "Dec 10 entry"),
                        BragEntry(LocalDateTime.of(2024, 12, 20, 10, 0), "Dec 20 entry"),
                        BragEntry(LocalDateTime.of(2024, 12, 28, 10, 0), "Dec 28 entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 3, 10, 0), "Jan 3 entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 8, 10, 0), "Jan 8 entry"),
                    ),
                    DateRange(LocalDate.of(2024, 12, 13), LocalDate.of(2025, 1, 10)),
                    4,
                    listOf("Dec 20 entry", "Dec 28 entry", "Jan 3 entry", "Jan 8 entry"),
                    listOf("Dec 10 entry"),
                ),
                Arguments.of(
                    "Q4 2024 query with Q1 2025 entries present",
                    listOf(
                        BragEntry(LocalDateTime.of(2024, 10, 15, 10, 0), "Oct 2024 Q4 entry"),
                        BragEntry(LocalDateTime.of(2024, 11, 20, 10, 0), "Nov 2024 Q4 entry"),
                        BragEntry(LocalDateTime.of(2024, 12, 28, 10, 0), "Dec 2024 Q4 entry"),
                        BragEntry(LocalDateTime.of(2025, 1, 5, 10, 0), "Jan 2025 Q1 entry"),
                    ),
                    DateRange(LocalDate.of(2024, 10, 1), LocalDate.of(2024, 12, 31)),
                    3,
                    listOf("Oct 2024 Q4 entry", "Nov 2024 Q4 entry", "Dec 2024 Q4 entry"),
                    listOf("Jan 2025 Q1 entry"),
                ),
                Arguments.of(
                    "Non-existent year files",
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 1, 15, 10, 0), "Jan 2025 entry"),
                    ),
                    DateRange(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                    0,
                    emptyList<String>(),
                    listOf("Jan 2025 entry"),
                ),
                Arguments.of(
                    "Three-year span",
                    listOf(
                        BragEntry(LocalDateTime.of(2023, 11, 15, 10, 0), "Nov 2023 entry"),
                        BragEntry(LocalDateTime.of(2024, 6, 15, 10, 0), "Jun 2024 entry"),
                        BragEntry(LocalDateTime.of(2025, 2, 15, 10, 0), "Feb 2025 entry"),
                    ),
                    DateRange(LocalDate.of(2023, 11, 1), LocalDate.of(2025, 2, 28)),
                    3,
                    listOf("Nov 2023 entry", "Jun 2024 entry", "Feb 2025 entry"),
                    emptyList<String>(),
                ),
            )
    }
}
