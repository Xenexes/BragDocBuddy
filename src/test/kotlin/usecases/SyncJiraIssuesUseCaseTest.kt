package usecases

import domain.DateRange
import domain.JiraIssue
import domain.JiraIssueSyncResult
import domain.PullRequest
import domain.Timeframe
import domain.TimeframeSpec
import domain.config.GitHubConfiguration
import domain.config.JiraConfiguration
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ports.BragRepository
import ports.GitHubClient
import ports.JiraClient
import ports.TimeframeParser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncJiraIssuesUseCaseTest {
    private lateinit var jiraClient: JiraClient
    private lateinit var bragRepository: BragRepository
    private lateinit var timeframeParser: TimeframeParser
    private lateinit var jiraConfig: JiraConfiguration
    private lateinit var gitHubClient: GitHubClient
    private lateinit var gitHubConfig: GitHubConfiguration
    private lateinit var useCase: SyncJiraIssuesUseCase

    @BeforeEach
    fun setup() {
        jiraClient = mockk()
        bragRepository = mockk()
        timeframeParser = mockk()
        jiraConfig = mockk()
        gitHubClient = mockk()
        gitHubConfig = mockk()
    }

    private fun createUseCase(withGitHub: Boolean = false) {
        useCase =
            if (withGitHub) {
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                    gitHubClient,
                    gitHubConfig,
                )
            } else {
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )
            }
    }

    @Test
    fun `should return Disabled when Jira issue sync is disabled`() =
        runTest {
            every { jiraConfig.enabled } returns false
            createUseCase()

            val result = useCase.syncJiraIssues(TimeframeSpec.Predefined(Timeframe.TODAY), printOnly = false)

            assertTrue(result is JiraIssueSyncResult.Disabled)
        }

    @Test
    fun `should return NotConfigured when not configured`() =
        runTest {
            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns false
            createUseCase()

            val result = useCase.syncJiraIssues(TimeframeSpec.Predefined(Timeframe.TODAY), printOnly = false)

            assertTrue(result is JiraIssueSyncResult.NotConfigured)
        }

    @Test
    fun `should return PrintOnly with issues when printOnly is true`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_MONTH)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val jiraIssues =
                listOf(
                    JiraIssue(
                        key = "PROJ-123",
                        title = "Test Issue",
                        url = "https://company.atlassian.net/browse/PROJ-123",
                        resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    JiraIssue(
                        key = "PROJ-124",
                        title = "Another Issue",
                        url = "https://company.atlassian.net/browse/PROJ-124",
                        resolvedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                )

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jiraIssues

            createUseCase()

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = true)

            assertTrue(result is JiraIssueSyncResult.PrintOnly)
            assertEquals(2, (result as JiraIssueSyncResult.PrintOnly).issues.size)
            assertEquals("PROJ-123", result.issues[0].key)
            assertEquals("PROJ-124", result.issues[1].key)
        }

    @Test
    fun `should return PrintOnly with empty list when no issues found and printOnly is true`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            createUseCase()

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = true)

            assertTrue(result is JiraIssueSyncResult.PrintOnly)
            assertEquals(0, (result as JiraIssueSyncResult.PrintOnly).issues.size)
        }

    @Test
    fun `should return ReadyToSync when printOnly is false and issues are found`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val jiraIssues =
                listOf(
                    JiraIssue(
                        key = "PROJ-123",
                        title = "Test Issue 1",
                        url = "https://company.atlassian.net/browse/PROJ-123",
                        resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    JiraIssue(
                        key = "PROJ-124",
                        title = "Test Issue 2",
                        url = "https://company.atlassian.net/browse/PROJ-124",
                        resolvedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                )

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jiraIssues

            createUseCase()

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            assertEquals(2, (result as JiraIssueSyncResult.ReadyToSync).issues.size)
        }

    @Test
    fun `should return ReadyToSync with empty list when no issues found and printOnly is false`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.TODAY)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            createUseCase()

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            assertEquals(0, (result as JiraIssueSyncResult.ReadyToSync).issues.size)
        }

    @Test
    fun `syncSelectedIssues should add all issues to brag document`() {
        val jiraIssues =
            listOf(
                JiraIssue(
                    key = "PROJ-123",
                    title = "Test Issue 1",
                    url = "https://company.atlassian.net/browse/PROJ-123",
                    resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                ),
                JiraIssue(
                    key = "PROJ-124",
                    title = "Test Issue 2",
                    url = "https://company.atlassian.net/browse/PROJ-124",
                    resolvedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                ),
            )

        every { bragRepository.save(any()) } returns true

        createUseCase()

        val result = useCase.syncSelectedIssues(jiraIssues)

        verify(exactly = 2) { bragRepository.save(any()) }
        assertTrue(result is JiraIssueSyncResult.Synced)
        assertEquals(2, result.addedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `syncSelectedIssues should handle duplicates correctly`() {
        val jiraIssues =
            listOf(
                JiraIssue(
                    key = "PROJ-123",
                    title = "Test Issue 1",
                    url = "https://company.atlassian.net/browse/PROJ-123",
                    resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                ),
                JiraIssue(
                    key = "PROJ-124",
                    title = "Test Issue 2",
                    url = "https://company.atlassian.net/browse/PROJ-124",
                    resolvedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                ),
                JiraIssue(
                    key = "PROJ-125",
                    title = "Test Issue 3",
                    url = "https://company.atlassian.net/browse/PROJ-125",
                    resolvedAt = LocalDateTime.of(2025, 1, 25, 9, 15),
                ),
            )

        every { bragRepository.save(match { it.content.contains("PROJ-123") }) } returns true
        every { bragRepository.save(match { it.content.contains("PROJ-124") }) } returns false
        every { bragRepository.save(match { it.content.contains("PROJ-125") }) } returns true

        createUseCase()

        val result = useCase.syncSelectedIssues(jiraIssues)

        verify(exactly = 3) { bragRepository.save(any()) }
        assertTrue(result is JiraIssueSyncResult.Synced)
        assertEquals(2, result.addedCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `syncSelectedIssues should handle empty list`() {
        createUseCase()

        val result = useCase.syncSelectedIssues(emptyList())

        verify(exactly = 0) { bragRepository.save(any()) }
        assertTrue(result is JiraIssueSyncResult.Synced)
        assertEquals(0, result.addedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `syncSelectedIssues should include status and issueType in brag entry`() {
        val issue =
            JiraIssue(
                key = "PROJ-123",
                title = "Test Issue",
                url = "https://company.atlassian.net/browse/PROJ-123",
                resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                status = "Done",
                issueType = "Story",
            )

        every { bragRepository.save(any()) } returns true

        createUseCase()
        useCase.syncSelectedIssues(listOf(issue))

        verify {
            bragRepository.save(
                match {
                    it.content ==
                        "[PROJ-123] Test Issue - https://company.atlassian.net/browse/PROJ-123 (Story, Done)"
                },
            )
        }
    }

    @Test
    fun `syncSelectedIssues should omit metadata when status and issueType are null`() {
        val issue =
            JiraIssue(
                key = "PROJ-123",
                title = "Test Issue",
                url = "https://company.atlassian.net/browse/PROJ-123",
                resolvedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
            )

        every { bragRepository.save(any()) } returns true

        createUseCase()
        useCase.syncSelectedIssues(listOf(issue))

        verify {
            bragRepository.save(
                match {
                    it.content == "[PROJ-123] Test Issue - https://company.atlassian.net/browse/PROJ-123"
                },
            )
        }
    }

    @Test
    fun `should extract JIRA keys from PRs and merge with JQL results`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_MONTH)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            val jqlIssues =
                listOf(
                    JiraIssue(
                        key = "PROJ-100",
                        title = "JQL Issue",
                        url = "https://company.atlassian.net/browse/PROJ-100",
                        resolvedAt = LocalDateTime.of(2025, 1, 10, 10, 0),
                        status = "Done",
                        issueType = "Story",
                    ),
                )

            val pullRequests =
                listOf(
                    PullRequest(
                        number = 1,
                        title = "PROJ-200 Add feature",
                        url = "https://github.com/org/repo/pull/1",
                        mergedAt = LocalDateTime.of(2025, 1, 12, 10, 0),
                        branchName = "feature/PROJ-200-add-feature",
                    ),
                    PullRequest(
                        number = 2,
                        title = "Fix bug",
                        url = "https://github.com/org/repo/pull/2",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                        description = "Fixes PROJ-100 and PROJ-300",
                        branchName = "fix/PROJ-300-bug",
                    ),
                )

            val prExtractedIssues =
                listOf(
                    JiraIssue(
                        key = "PROJ-200",
                        title = "PR-extracted issue",
                        url = "https://company.atlassian.net/browse/PROJ-200",
                        resolvedAt = LocalDateTime.of(2025, 1, 12, 10, 0),
                        status = "Done",
                        issueType = "Task",
                    ),
                    JiraIssue(
                        key = "PROJ-300",
                        title = "Another PR-extracted issue",
                        url = "https://company.atlassian.net/browse/PROJ-300",
                        resolvedAt = LocalDateTime.of(2025, 1, 20, 10, 0),
                        status = "In Review",
                        issueType = "Bug",
                    ),
                )

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jqlIssues

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests
            coEvery {
                jiraClient.fetchIssuesByKeys(setOf("PROJ-200", "PROJ-300"))
            } returns prExtractedIssues

            createUseCase(withGitHub = true)

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            val issues = (result as JiraIssueSyncResult.ReadyToSync).issues
            assertEquals(3, issues.size)
            assertEquals(setOf("PROJ-100", "PROJ-200", "PROJ-300"), issues.map { it.key }.toSet())
        }

    @Test
    fun `should not fetch PRs when GitHub is not configured`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_MONTH)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            createUseCase()

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            assertEquals(0, (result as JiraIssueSyncResult.ReadyToSync).issues.size)
        }

    @Test
    fun `should not fetch PRs when GitHub is disabled`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_MONTH)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            every { gitHubConfig.enabled } returns false

            createUseCase(withGitHub = true)

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            assertEquals(0, (result as JiraIssueSyncResult.ReadyToSync).issues.size)
        }

    @Test
    fun `should not fetch additional issues when all PR keys already in JQL results`() =
        runTest {
            val timeframeSpec = TimeframeSpec.Predefined(Timeframe.LAST_MONTH)
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            val jqlIssues =
                listOf(
                    JiraIssue(
                        key = "PROJ-123",
                        title = "JQL Issue",
                        url = "https://company.atlassian.net/browse/PROJ-123",
                        resolvedAt = LocalDateTime.of(2025, 1, 10, 10, 0),
                    ),
                )

            val pullRequests =
                listOf(
                    PullRequest(
                        number = 1,
                        title = "PROJ-123 already found by JQL",
                        url = "https://github.com/org/repo/pull/1",
                        mergedAt = LocalDateTime.of(2025, 1, 12, 10, 0),
                    ),
                )

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(timeframeSpec) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jqlIssues

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests

            createUseCase(withGitHub = true)

            val result = useCase.syncJiraIssues(timeframeSpec, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            val issues = (result as JiraIssueSyncResult.ReadyToSync).issues
            assertEquals(1, issues.size)
            assertEquals("PROJ-123", issues[0].key)
        }
}
