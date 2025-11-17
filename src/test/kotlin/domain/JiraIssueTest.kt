package domain

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class JiraIssueTest {
    @Test
    fun `should create JiraIssue with all fields`() {
        val key = "PROJ-123"
        val title = "Implement new feature"
        val url = "https://company.atlassian.net/browse/PROJ-123"
        val resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 30)

        val jiraIssue =
            JiraIssue(
                key = key,
                title = title,
                url = url,
                resolvedAt = resolvedAt,
            )

        assertEquals(key, jiraIssue.key)
        assertEquals(title, jiraIssue.title)
        assertEquals(url, jiraIssue.url)
        assertEquals(resolvedAt, jiraIssue.resolvedAt)
    }

    @Test
    fun `should create multiple JiraIssue instances`() {
        val issue1 = JiraIssue("PROJ-1", "First", "url1", LocalDateTime.now())
        val issue2 = JiraIssue("PROJ-2", "Second", "url2", LocalDateTime.now())

        assertEquals("PROJ-1", issue1.key)
        assertEquals("PROJ-2", issue2.key)
    }
}
