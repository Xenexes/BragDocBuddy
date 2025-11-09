package domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BragEntryTest {
    @Test
    fun `should create brag entry with timestamp and content`() {
        val timestamp = LocalDateTime.of(2025, 1, 15, 14, 30, 0)
        val content = "Completed feature implementation"

        val bragEntry = BragEntry(timestamp, content)

        assertThat(bragEntry.timestamp).isEqualTo(timestamp)
        assertThat(bragEntry.content).isEqualTo(content)
    }

    @Test
    fun `should extract date from timestamp`() {
        val timestamp = LocalDateTime.of(2025, 1, 15, 14, 30, 0)
        val expectedDate = LocalDate.of(2025, 1, 15)
        val content = "Test entry"

        val bragEntry = BragEntry(timestamp, content)

        assertThat(bragEntry.date).isEqualTo(expectedDate)
    }

    @Test
    fun `should handle different times on same date`() {
        val morningTimestamp = LocalDateTime.of(2025, 1, 15, 9, 0, 0)
        val eveningTimestamp = LocalDateTime.of(2025, 1, 15, 18, 0, 0)
        val expectedDate = LocalDate.of(2025, 1, 15)

        val morningEntry = BragEntry(morningTimestamp, "Morning achievement")
        val eveningEntry = BragEntry(eveningTimestamp, "Evening achievement")

        assertThat(morningEntry.date).isEqualTo(expectedDate)
        assertThat(eveningEntry.date).isEqualTo(expectedDate)
    }

    @Test
    fun `should handle empty content`() {
        val timestamp = LocalDateTime.now()
        val content = ""

        val bragEntry = BragEntry(timestamp, content)

        assertThat(bragEntry.content).isEqualTo("")
        assertThat(bragEntry.timestamp).isNotNull()
    }

    @Test
    fun `should handle long content`() {
        val timestamp = LocalDateTime.now()
        val longContent =
            @Suppress("ktlint:standard:max-line-length")
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."

        val bragEntry = BragEntry(timestamp, longContent)

        assertThat(bragEntry.content).isEqualTo(longContent)
        assertThat(bragEntry.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `should be equal when timestamp and content are the same`() {
        val timestamp = LocalDateTime.of(2025, 1, 15, 14, 30, 0)
        val content = "Same content"

        val entry1 = BragEntry(timestamp, content)
        val entry2 = BragEntry(timestamp, content)

        assertThat(entry1).isEqualTo(entry2)
    }
}
