package usecases

import domain.DateRange
import domain.JiraIssue
import domain.JiraIssueSyncResult
import domain.Timeframe
import infrastructure.jira.JiraConfiguration
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
import ports.JiraClient
import ports.TimeframeParser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncJiraIssuesUseCaseTest {
    private lateinit var jiraClient: JiraClient
    private lateinit var bragRepository: BragRepository
    private lateinit var timeframeParser: TimeframeParser
    private lateinit var jiraConfig: JiraConfiguration
    private lateinit var useCase: SyncJiraIssuesUseCase

    @BeforeEach
    fun setup() {
        jiraClient = mockk()
        bragRepository = mockk()
        timeframeParser = mockk()
        jiraConfig = mockk()
    }

    @Test
    fun `should return Disabled when Jira issue sync is disabled`() =
        runTest {
            every { jiraConfig.enabled } returns false
            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.TODAY, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.Disabled)
        }

    @Test
    fun `should return NotConfigured when not configured`() =
        runTest {
            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns false
            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.TODAY, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.NotConfigured)
        }

    @Test
    fun `should return PrintOnly with issues when printOnly is true`() =
        runTest {
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
            every { timeframeParser.parse(Timeframe.LAST_MONTH) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jiraIssues

            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.LAST_MONTH, printOnly = true)

            assertTrue(result is JiraIssueSyncResult.PrintOnly)
            assertEquals(2, (result as JiraIssueSyncResult.PrintOnly).issues.size)
            assertEquals("PROJ-123", result.issues[0].key)
            assertEquals("PROJ-124", result.issues[1].key)
        }

    @Test
    fun `should return PrintOnly with empty list when no issues found and printOnly is true`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.TODAY, printOnly = true)

            assertTrue(result is JiraIssueSyncResult.PrintOnly)
            assertEquals(0, (result as JiraIssueSyncResult.PrintOnly).issues.size)
        }

    @Test
    fun `should return ReadyToSync when printOnly is false and issues are found`() =
        runTest {
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
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns jiraIssues

            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.TODAY, printOnly = false)

            assertTrue(result is JiraIssueSyncResult.ReadyToSync)
            assertEquals(2, (result as JiraIssueSyncResult.ReadyToSync).issues.size)
        }

    @Test
    fun `should return ReadyToSync with empty list when no issues found and printOnly is false`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { jiraConfig.enabled } returns true
            every { jiraConfig.isConfigured() } returns true
            every { jiraConfig.email } returns "test@example.com"
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                jiraClient.fetchResolvedIssues("test@example.com", dateRange)
            } returns emptyList()

            useCase =
                SyncJiraIssuesUseCase(
                    jiraClient,
                    bragRepository,
                    timeframeParser,
                    jiraConfig,
                )

            val result = useCase.syncJiraIssues(Timeframe.TODAY, printOnly = false)

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

        useCase =
            SyncJiraIssuesUseCase(
                jiraClient,
                bragRepository,
                timeframeParser,
                jiraConfig,
            )

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

        useCase =
            SyncJiraIssuesUseCase(
                jiraClient,
                bragRepository,
                timeframeParser,
                jiraConfig,
            )

        val result = useCase.syncSelectedIssues(jiraIssues)

        verify(exactly = 3) { bragRepository.save(any()) }
        assertTrue(result is JiraIssueSyncResult.Synced)
        assertEquals(2, result.addedCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun `syncSelectedIssues should handle empty list`() {
        useCase =
            SyncJiraIssuesUseCase(
                jiraClient,
                bragRepository,
                timeframeParser,
                jiraConfig,
            )

        val result = useCase.syncSelectedIssues(emptyList())

        verify(exactly = 0) { bragRepository.save(any()) }
        assertTrue(result is JiraIssueSyncResult.Synced)
        assertEquals(0, result.addedCount)
        assertEquals(0, result.skippedCount)
    }
}
