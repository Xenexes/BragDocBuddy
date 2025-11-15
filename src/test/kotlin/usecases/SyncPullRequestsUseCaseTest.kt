package usecases

import domain.BragEntry
import domain.DateRange
import domain.PullRequest
import domain.PullRequestSyncResult
import domain.Timeframe
import infrastructure.github.GitHubConfiguration
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ports.BragRepository
import ports.GitHubClient
import ports.TimeframeParser
import java.time.LocalDate
import java.time.LocalDateTime

class SyncPullRequestsUseCaseTest {
    private lateinit var gitHubClient: GitHubClient
    private lateinit var bragRepository: BragRepository
    private lateinit var timeframeParser: TimeframeParser
    private lateinit var gitHubConfig: GitHubConfiguration
    private lateinit var useCase: SyncPullRequestsUseCase

    @BeforeEach
    fun setup() {
        gitHubClient = mockk()
        bragRepository = mockk()
        timeframeParser = mockk()
        gitHubConfig = mockk()
    }

    @Test
    fun `should return Disabled when GitHub PR sync is disabled`() =
        runTest {
            every { gitHubConfig.enabled } returns false
            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.TODAY, printOnly = false)

            assertTrue(result is PullRequestSyncResult.Disabled)
        }

    @Test
    fun `should return NotConfigured when not configured`() =
        runTest {
            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns false
            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.TODAY, printOnly = false)

            assertTrue(result is PullRequestSyncResult.NotConfigured)
        }

    @Test
    fun `should return PrintOnly with pull requests when printOnly is true`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val pullRequests =
                listOf(
                    PullRequest(
                        number = 123,
                        title = "Test PR",
                        url = "https://github.com/org/repo/pull/123",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    PullRequest(
                        number = 124,
                        title = "Another PR",
                        url = "https://github.com/org/repo/pull/124",
                        mergedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                )

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.LAST_MONTH) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.LAST_MONTH, printOnly = true)

            assertTrue(result is PullRequestSyncResult.PrintOnly)
            assertEquals(2, (result as PullRequestSyncResult.PrintOnly).pullRequests.size)
            assertEquals(123, result.pullRequests[0].number)
            assertEquals(124, result.pullRequests[1].number)
        }

    @Test
    fun `should return PrintOnly with empty list when no PRs found and printOnly is true`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns emptyList()

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.TODAY, printOnly = true)

            assertTrue(result is PullRequestSyncResult.PrintOnly)
            assertEquals(0, (result as PullRequestSyncResult.PrintOnly).pullRequests.size)
        }

    @Test
    fun `should enrich brag document and return Synced when printOnly is false`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val pullRequests =
                listOf(
                    PullRequest(
                        number = 123,
                        title = "Test PR",
                        url = "https://github.com/org/repo/pull/123",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                )

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests

            val entrySlot = slot<BragEntry>()
            every { bragRepository.save(capture(entrySlot)) } returns true

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.TODAY, printOnly = false)

            verify(exactly = 1) { bragRepository.save(any()) }
            val capturedEntry = entrySlot.captured
            assertTrue(capturedEntry.content.contains("[PR #123]"))
            assertTrue(capturedEntry.content.contains("Test PR"))
            assertTrue(capturedEntry.content.contains("https://github.com/org/repo/pull/123"))

            assertTrue(result is PullRequestSyncResult.Synced)
            assertEquals(1, (result as PullRequestSyncResult.Synced).addedCount)
            assertEquals(0, result.skippedCount)
        }

    @Test
    fun `should return Synced with zero counts when no PRs found and printOnly is false`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.TODAY) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns emptyList()

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.TODAY, printOnly = false)

            assertTrue(result is PullRequestSyncResult.Synced)
            assertEquals(0, (result as PullRequestSyncResult.Synced).addedCount)
            assertEquals(0, result.skippedCount)
        }

    @Test
    fun `should report when all pull requests are duplicates`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val pullRequests =
                listOf(
                    PullRequest(
                        number = 123,
                        title = "Test PR",
                        url = "https://github.com/org/repo/pull/123",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    PullRequest(
                        number = 124,
                        title = "Another PR",
                        url = "https://github.com/org/repo/pull/124",
                        mergedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                )

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.LAST_MONTH) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests
            every { bragRepository.save(any()) } returns false

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.LAST_MONTH, printOnly = false)

            assertTrue(result is PullRequestSyncResult.Synced)
            assertEquals(0, (result as PullRequestSyncResult.Synced).addedCount)
            assertEquals(2, result.skippedCount)
        }

    @Test
    fun `should report mixed results when some PRs are duplicates`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val pullRequests =
                listOf(
                    PullRequest(
                        number = 123,
                        title = "New PR",
                        url = "https://github.com/org/repo/pull/123",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    PullRequest(
                        number = 124,
                        title = "Duplicate PR",
                        url = "https://github.com/org/repo/pull/124",
                        mergedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                    PullRequest(
                        number = 125,
                        title = "Another New PR",
                        url = "https://github.com/org/repo/pull/125",
                        mergedAt = LocalDateTime.of(2025, 1, 25, 9, 15),
                    ),
                )

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.LAST_MONTH) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests

            every { bragRepository.save(match { it.content.contains("#123") }) } returns true
            every { bragRepository.save(match { it.content.contains("#124") }) } returns false
            every { bragRepository.save(match { it.content.contains("#125") }) } returns true

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.LAST_MONTH, printOnly = false)

            assertTrue(result is PullRequestSyncResult.Synced)
            assertEquals(2, (result as PullRequestSyncResult.Synced).addedCount)
            assertEquals(1, result.skippedCount)
        }

    @Test
    fun `should handle multiple PRs added successfully`() =
        runTest {
            val dateRange = DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))
            val pullRequests =
                listOf(
                    PullRequest(
                        number = 123,
                        title = "Test PR 1",
                        url = "https://github.com/org/repo/pull/123",
                        mergedAt = LocalDateTime.of(2025, 1, 15, 10, 0),
                    ),
                    PullRequest(
                        number = 124,
                        title = "Test PR 2",
                        url = "https://github.com/org/repo/pull/124",
                        mergedAt = LocalDateTime.of(2025, 1, 20, 14, 30),
                    ),
                )

            every { gitHubConfig.enabled } returns true
            every { gitHubConfig.isConfigured() } returns true
            every { gitHubConfig.organization } returns "test-org"
            every { gitHubConfig.username } returns "test-user"
            every { timeframeParser.parse(Timeframe.LAST_MONTH) } returns dateRange
            coEvery {
                gitHubClient.fetchMergedPullRequests("test-org", "test-user", dateRange)
            } returns pullRequests
            every { bragRepository.save(any()) } returns true

            useCase =
                SyncPullRequestsUseCase(
                    gitHubClient,
                    bragRepository,
                    timeframeParser,
                    gitHubConfig,
                )

            val result = useCase.syncPullRequests(Timeframe.LAST_MONTH, printOnly = false)

            assertTrue(result is PullRequestSyncResult.Synced)
            assertEquals(2, (result as PullRequestSyncResult.Synced).addedCount)
            assertEquals(0, result.skippedCount)
        }
}
