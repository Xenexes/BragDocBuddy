package infrastructure.persistence

import domain.BragEntry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownBragFileHandlerTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var handler: MarkdownBragFileHandler
    private lateinit var testFile: File

    @BeforeEach
    fun setup() {
        handler = MarkdownBragFileHandler()
        testFile = File(tempDir.toFile(), "test-brags.md")
    }

    @Test
    fun `should parse all entries from markdown file`() {
        testFile.writeText(
            """
            # Brags 2025

            ## 2025-11-01
            * 10:00:00 First entry
            * 15:30:00 Second entry

            ## 2025-11-02
            * 09:00:00 Third entry

            """.trimIndent(),
        )

        val entriesByDate = handler.parseAllEntries(testFile)

        assertEquals(2, entriesByDate.size)
        assertEquals(2, entriesByDate[LocalDate.of(2025, 11, 1)]?.size)
        assertEquals(1, entriesByDate[LocalDate.of(2025, 11, 2)]?.size)
        assertEquals("First entry", entriesByDate[LocalDate.of(2025, 11, 1)]?.get(0)?.content)
    }

    @Test
    fun `should return empty map when file does not exist`() {
        val nonExistentFile = File(tempDir.toFile(), "non-existent.md")

        val entriesByDate = handler.parseAllEntries(nonExistentFile)

        assertTrue(entriesByDate.isEmpty())
    }

    @Test
    fun `should parse entries in date range`() {
        testFile.writeText(
            """
            # Brags 2025

            ## 2025-11-01
            * 10:00:00 Entry on Nov 1

            ## 2025-11-05
            * 14:00:00 Entry on Nov 5

            ## 2025-11-10
            * 09:00:00 Entry on Nov 10

            """.trimIndent(),
        )

        val entries =
            handler.parseEntriesInRange(testFile) { date ->
                date >= LocalDate.of(2025, 11, 1) && date <= LocalDate.of(2025, 11, 5)
            }

        assertEquals(2, entries.size)
        assertTrue(entries.any { it.content == "Entry on Nov 1" })
        assertTrue(entries.any { it.content == "Entry on Nov 5" })
        assertTrue(entries.none { it.content == "Entry on Nov 10" })
    }

    @Test
    fun `should format entries correctly`() {
        val entriesByDate =
            mapOf(
                LocalDate.of(2025, 11, 1) to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 11, 1, 10, 0, 0), "First entry"),
                        BragEntry(LocalDateTime.of(2025, 11, 1, 15, 30, 0), "Second entry"),
                    ),
                LocalDate.of(2025, 11, 2) to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 11, 2, 9, 0, 0), "Third entry"),
                    ),
            )

        val content = handler.formatEntries(2025, entriesByDate)

        val lines = content.lines()
        assertTrue(lines.contains("# Brags 2025"))
        assertTrue(lines.contains("## 2025-11-01"))
        assertTrue(lines.contains("* 10:00:00 First entry"))
        assertTrue(lines.contains("* 15:30:00 Second entry"))
        assertTrue(lines.contains("## 2025-11-02"))
        assertTrue(lines.contains("* 09:00:00 Third entry"))
    }

    @Test
    fun `should sort dates chronologically when formatting`() {
        val entriesByDate =
            mapOf(
                LocalDate.of(2025, 11, 5) to
                    listOf(BragEntry(LocalDateTime.of(2025, 11, 5, 10, 0, 0), "Nov 5")),
                LocalDate.of(2025, 11, 1) to
                    listOf(BragEntry(LocalDateTime.of(2025, 11, 1, 10, 0, 0), "Nov 1")),
                LocalDate.of(2025, 11, 3) to
                    listOf(BragEntry(LocalDateTime.of(2025, 11, 3, 10, 0, 0), "Nov 3")),
            )

        val content = handler.formatEntries(2025, entriesByDate)
        val lines = content.lines()

        val nov1Index = lines.indexOfFirst { it == "## 2025-11-01" }
        val nov3Index = lines.indexOfFirst { it == "## 2025-11-03" }
        val nov5Index = lines.indexOfFirst { it == "## 2025-11-05" }

        assertTrue(nov1Index < nov3Index)
        assertTrue(nov3Index < nov5Index)
    }

    @Test
    fun `should sort entries by time within each date when formatting`() {
        val entriesByDate =
            mapOf(
                LocalDate.of(2025, 11, 1) to
                    listOf(
                        BragEntry(LocalDateTime.of(2025, 11, 1, 15, 0, 0), "Third"),
                        BragEntry(LocalDateTime.of(2025, 11, 1, 10, 0, 0), "First"),
                        BragEntry(LocalDateTime.of(2025, 11, 1, 12, 0, 0), "Second"),
                    ),
            )

        val content = handler.formatEntries(2025, entriesByDate)
        val lines = content.lines()

        val firstIndex = lines.indexOfFirst { it.contains("First") }
        val secondIndex = lines.indexOfFirst { it.contains("Second") }
        val thirdIndex = lines.indexOfFirst { it.contains("Third") }

        assertTrue(firstIndex < secondIndex)
        assertTrue(secondIndex < thirdIndex)
    }

    @Test
    fun `should skip malformed date headers during parsing`() {
        testFile.writeText(
            """
            # Brags 2025

            ## 2025-11-01
            * 10:00:00 Valid entry

            ## invalid-date-format
            * 12:00:00 This should be skipped

            ## 2025-11-02
            * 14:00:00 Another valid entry

            """.trimIndent(),
        )

        val entriesByDate = handler.parseAllEntries(testFile)

        assertEquals(2, entriesByDate.size)
        assertTrue(entriesByDate.containsKey(LocalDate.of(2025, 11, 1)))
        assertTrue(entriesByDate.containsKey(LocalDate.of(2025, 11, 2)))
    }

    @Test
    fun `should skip malformed entry lines during parsing`() {
        testFile.writeText(
            """
            # Brags 2025

            ## 2025-11-01
            * 10:00:00 Valid entry
            * invalid-time-format This should be skipped
            * 15:00:00 Another valid entry

            """.trimIndent(),
        )

        val entriesByDate = handler.parseAllEntries(testFile)

        val entries = entriesByDate[LocalDate.of(2025, 11, 1)]
        assertEquals(2, entries?.size)
        assertEquals("Valid entry", entries?.get(0)?.content)
        assertEquals("Another valid entry", entries?.get(1)?.content)
    }

    @Test
    fun `should handle empty file`() {
        testFile.writeText("")

        val entriesByDate = handler.parseAllEntries(testFile)

        assertTrue(entriesByDate.isEmpty())
    }

    @Test
    fun `should round-trip parse and format correctly`() {
        val originalContent =
            """
            # Brags 2025

            ## 2025-11-01
            * 10:00:00 First entry
            * 15:30:00 Second entry

            ## 2025-11-02
            * 09:00:00 Third entry

            """.trimIndent()
        testFile.writeText(originalContent)

        val entriesByDate = handler.parseAllEntries(testFile)
        val formattedContent = handler.formatEntries(2025, entriesByDate)

        assertEquals(originalContent + "\n", formattedContent)
    }
}
