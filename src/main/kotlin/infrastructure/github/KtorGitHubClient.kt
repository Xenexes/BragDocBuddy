package infrastructure.github

import domain.DateRange
import domain.PullRequest
import domain.config.GitHubConfiguration
import infrastructure.github.dto.GitHubErrorDto
import infrastructure.github.dto.GitHubPullRequestDto
import infrastructure.github.dto.GitHubSearchResponseDto
import infrastructure.http.HttpClientFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import ports.GitHubClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

class KtorGitHubClient(
    private val configuration: GitHubConfiguration,
    httpClientFactory: HttpClientFactory,
) : GitHubClient,
    AutoCloseable {
    companion object {
        private const val GITHUB_API_PAGE_SIZE = 100
        private const val GITHUB_API_MAX_PAGES = 10
        private const val GITHUB_API_VERSION = "2022-11-28"
        private const val GITHUB_API_BASE_URL = "https://api.github.com"
    }

    private val client = httpClientFactory.create()

    override suspend fun fetchMergedPullRequests(
        organization: String,
        author: String,
        dateRange: DateRange,
    ): List<PullRequest> {
        val allPullRequests = mutableListOf<GitHubPullRequestDto>()
        var page = 1

        logger.info { "Fetching merged PRs for $author in $organization from ${dateRange.start} to ${dateRange.end}" }

        while (true) {
            val searchQuery =
                buildString {
                    append("is:pr")
                    append(" org:$organization")
                    append(" author:$author")
                    append(" archived:false")
                    append(" created:${dateRange.start}..${dateRange.end}")
                }

            if (page == 1) {
                logger.info { "Search query: $searchQuery" }
            }

            val httpResponse: HttpResponse =
                client.get("$GITHUB_API_BASE_URL/search/issues") {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${configuration.token}")
                        append(HttpHeaders.Accept, "application/vnd.github+json")
                        append("X-GitHub-Api-Version", GITHUB_API_VERSION)
                    }
                    parameter("q", searchQuery)
                    parameter("sort", "created")
                    parameter("order", "desc")
                    parameter("per_page", GITHUB_API_PAGE_SIZE)
                    parameter("page", page)
                }

            if (httpResponse.status.value !in 200..299) {
                logger.error { "GitHub API error: HTTP ${httpResponse.status.value} ${httpResponse.status.description}" }
                val errorBody = httpResponse.bodyAsText()
                logger.error { "Response body: $errorBody" }

                try {
                    val error = Json.decodeFromString<GitHubErrorDto>(errorBody)
                    throw IllegalStateException("GitHub API error: ${error.message}")
                } catch (e: Exception) {
                    throw IllegalStateException("GitHub API error: HTTP ${httpResponse.status.value}")
                }
            }

            val response: GitHubSearchResponseDto = httpResponse.body()

            logger.info { "Page $page: Found ${response.items.size} PRs (Total: ${response.totalCount})" }

            if (response.items.isEmpty()) break

            allPullRequests.addAll(response.items)

            if (response.items.size < GITHUB_API_PAGE_SIZE || allPullRequests.size >= response.totalCount) break

            page++

            if (page > GITHUB_API_MAX_PAGES) {
                logger.warn { "Reached API pagination limit (${GITHUB_API_MAX_PAGES * GITHUB_API_PAGE_SIZE} results max)" }
                break
            }
        }

        return allPullRequests
            .filter { it.pullRequest?.mergedAt != null }
            .map { it.toDomain() }
            .sortedBy { it.mergedAt }
    }

    private fun GitHubPullRequestDto.toDomain(): PullRequest {
        val mergedAtInstant = Instant.parse(pullRequest!!.mergedAt!!)
        val mergedAtDateTime = LocalDateTime.ofInstant(mergedAtInstant, ZoneOffset.UTC)

        return PullRequest(
            number = number,
            title = title,
            url = htmlUrl,
            mergedAt = mergedAtDateTime,
        )
    }

    override fun close() {
        logger.debug { "Closing GitHub HTTP client" }
        client.close()
    }
}
