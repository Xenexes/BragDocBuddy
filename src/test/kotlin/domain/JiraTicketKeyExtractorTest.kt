package domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JiraTicketKeyExtractorTest {
    @Test
    fun `should extract key from PR title`() {
        val keys = JiraTicketKeyExtractor.extractKeys("PROJ-123 Add login feature")

        assertEquals(setOf("PROJ-123"), keys)
    }

    @Test
    fun `should extract key from PR title with brackets`() {
        val keys = JiraTicketKeyExtractor.extractKeys("[PROJ-456] Fix authentication bug")

        assertEquals(setOf("PROJ-456"), keys)
    }

    @Test
    fun `should extract multiple keys from PR description`() {
        val keys =
            JiraTicketKeyExtractor.extractKeys(
                "This PR fixes PROJ-123 and also addresses PROJ-456",
            )

        assertEquals(setOf("PROJ-123", "PROJ-456"), keys)
    }

    @Test
    fun `should extract key from branch name`() {
        val keys = JiraTicketKeyExtractor.extractKeys("feature/PROJ-789-add-login")

        assertEquals(setOf("PROJ-789"), keys)
    }

    @Test
    fun `should extract keys from multiple texts`() {
        val keys =
            JiraTicketKeyExtractor.extractKeys(
                "PROJ-123 title",
                "Fixes TEAM-456",
                "feature/CORE-789-branch",
            )

        assertEquals(setOf("PROJ-123", "TEAM-456", "CORE-789"), keys)
    }

    @Test
    fun `should handle null texts`() {
        val keys = JiraTicketKeyExtractor.extractKeys(null, "PROJ-123", null)

        assertEquals(setOf("PROJ-123"), keys)
    }

    @Test
    fun `should return empty set when no keys found`() {
        val keys = JiraTicketKeyExtractor.extractKeys("No jira keys here")

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should return empty set for all null texts`() {
        val keys = JiraTicketKeyExtractor.extractKeys(null, null)

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should return empty set for empty varargs`() {
        val keys = JiraTicketKeyExtractor.extractKeys()

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should deduplicate keys found in multiple texts`() {
        val keys =
            JiraTicketKeyExtractor.extractKeys(
                "PROJ-123 in title",
                "Also mentions PROJ-123 in description",
            )

        assertEquals(setOf("PROJ-123"), keys)
    }

    @Test
    fun `should handle project keys with numbers`() {
        val keys = JiraTicketKeyExtractor.extractKeys("P2P-42 peer to peer feature")

        assertEquals(setOf("P2P-42"), keys)
    }

    @Test
    fun `should not match lowercase keys`() {
        val keys = JiraTicketKeyExtractor.extractKeys("proj-123 is not valid")

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should not match keys starting with number`() {
        val keys = JiraTicketKeyExtractor.extractKeys("1PROJ-123 invalid prefix")

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should handle project keys with underscores`() {
        val keys = JiraTicketKeyExtractor.extractKeys("MY_PROJ-42 has underscores")

        assertEquals(setOf("MY_PROJ-42"), keys)
    }

    @Test
    fun `should not match keys with leading zeros in number`() {
        val keys = JiraTicketKeyExtractor.extractKeys("PROJ-007 has leading zero")

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should not match single character project key`() {
        val keys = JiraTicketKeyExtractor.extractKeys("A-1 too short")

        assertTrue(keys.isEmpty())
    }

    @Test
    fun `should match two character project key`() {
        val keys = JiraTicketKeyExtractor.extractKeys("AB-1 minimum length")

        assertEquals(setOf("AB-1"), keys)
    }

    @Test
    fun `should extract key with large issue number`() {
        val keys = JiraTicketKeyExtractor.extractKeys("PROJ-99999 large number")

        assertEquals(setOf("PROJ-99999"), keys)
    }
}
