package infrastructure.jira

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JiraConfigurationTest {
    @Test
    fun `should have default JQL template`() {
        val config =
            JiraConfiguration(
                enabled = true,
                url = "https://company.atlassian.net",
                email = "test@example.com",
                apiToken = "test-token",
            )

        assertTrue(config.jqlTemplate.contains("{email}"))
        assertTrue(config.jqlTemplate.contains("{startDate}"))
        assertTrue(config.jqlTemplate.contains("{endDate}"))
        assertTrue(config.jqlTemplate.contains("assignee ="))
        assertTrue(config.jqlTemplate.contains("Engineer[User Picker (single user)]"))
        assertTrue(config.jqlTemplate.contains("statusCategory IN (Done)"))
        assertTrue(config.jqlTemplate.contains("status was \"In Progress\""))
    }

    @Test
    fun `should allow custom JQL template`() {
        val customTemplate = "assignee = \"{email}\" AND created >= \"{startDate}\""
        val config =
            JiraConfiguration(
                enabled = true,
                url = "https://company.atlassian.net",
                email = "test@example.com",
                apiToken = "test-token",
                jqlTemplate = customTemplate,
            )

        assertEquals(customTemplate, config.jqlTemplate)
    }

    @Test
    fun `should be configured when all required fields are present`() {
        val config =
            JiraConfiguration(
                enabled = true,
                url = "https://company.atlassian.net",
                email = "test@example.com",
                apiToken = "test-token",
            )

        assertTrue(config.isConfigured())
    }

    @Test
    fun `should not be configured when URL is missing`() {
        val config =
            JiraConfiguration(
                enabled = true,
                url = null,
                email = "test@example.com",
                apiToken = "test-token",
            )

        assertFalse(config.isConfigured())
    }

    @Test
    fun `should not be configured when email is missing`() {
        val config =
            JiraConfiguration(
                enabled = true,
                url = "https://company.atlassian.net",
                email = null,
                apiToken = "test-token",
            )

        assertFalse(config.isConfigured())
    }

    @Test
    fun `should not be configured when API token is missing`() {
        val config =
            JiraConfiguration(
                enabled = true,
                url = "https://company.atlassian.net",
                email = "test@example.com",
                apiToken = null,
            )

        assertFalse(config.isConfigured())
    }

    @Test
    fun `default JQL template should contain required elements`() {
        val template = JiraConfiguration.DEFAULT_JQL_TEMPLATE

        assertTrue(template.contains("assignee = \"{email}\""))
        assertTrue(template.contains("\"Engineer[User Picker (single user)]\" = \"{email}\""))
        assertTrue(template.contains("assignee WAS \"{email}\" DURING (\"{startDate}\", \"{endDate}\")"))
        assertTrue(template.contains("status was \"In Progress\""))
        assertTrue(template.contains("statusCategory IN (Done)"))
        assertTrue(template.contains("\"Last Transition Occurred[Date]\" >= \"{startDate}\""))
        assertTrue(template.contains("\"Last Transition Occurred[Date]\" <= \"{endDate}\""))
    }
}
