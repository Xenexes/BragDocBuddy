package infrastructure.github

import domain.DateRange
import domain.PullRequest
import domain.config.GitHubConfiguration
import infrastructure.github.dto.GitHubErrorDto
import infrastructure.github.dto.GraphQLRequestDto
import infrastructure.github.dto.GraphQLResponseDto
import infrastructure.github.dto.PullRequestNodeDto
import infrastructure.http.HttpClientFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
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
        private const val GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"
        private const val MAX_DESCRIPTION_LENGTH = 2500

        private val SEARCH_QUERY =
            """
            query(${'$'}query: String!, ${'$'}cursor: String) {
              search(type: ISSUE, query: ${'$'}query, first: $GITHUB_API_PAGE_SIZE, after: ${'$'}cursor) {
                pageInfo {
                  hasNextPage
                  endCursor
                }
                nodes {
                  ... on PullRequest {
                    number
                    title
                    url
                    mergedAt
                    body
                  }
                }
              }
            }
            """.trimIndent()
    }

    private val client = httpClientFactory.create()

    override suspend fun fetchMergedPullRequests(
        organization: String,
        author: String,
        dateRange: DateRange,
    ): List<PullRequest> {
        val allNodes = mutableListOf<PullRequestNodeDto>()
        var cursor: String? = null
        var pageCount = 0

        val searchQuery =
            buildString {
                append("is:pr is:merged")
                append(" org:$organization")
                append(" author:$author")
                append(" archived:false")
                append(" created:${dateRange.start}..${dateRange.end}")
            }

        logger.info { "Fetching merged PRs for $author in $organization from ${dateRange.start} to ${dateRange.end}" }
        logger.info { "Search query: $searchQuery" }

        while (true) {
            pageCount++

            val requestBody =
                GraphQLRequestDto(
                    query = SEARCH_QUERY,
                    variables = mapOf("query" to searchQuery, "cursor" to cursor),
                )

            val httpResponse: HttpResponse =
                client.post(GITHUB_GRAPHQL_URL) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${configuration.token}")
                    }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            if (httpResponse.status.value !in 200..299) {
                handleHttpError(httpResponse)
            }

            val response: GraphQLResponseDto = httpResponse.body()

            if (!response.errors.isNullOrEmpty()) {
                val errorMessages = response.errors.joinToString("; ") { it.message }
                logger.error { "GraphQL errors: $errorMessages" }
                throw IllegalStateException("GitHub GraphQL error: $errorMessages")
            }

            val searchResult =
                response.data?.search
                    ?: throw IllegalStateException("GitHub GraphQL error: no data in response")

            val nodes = searchResult.nodes.filterNotNull()
            logger.info { "Page $pageCount: Found ${nodes.size} PRs" }

            allNodes.addAll(nodes)

            if (!searchResult.pageInfo.hasNextPage) break

            cursor = searchResult.pageInfo.endCursor

            if (pageCount >= GITHUB_API_MAX_PAGES) {
                logger.warn { "Reached API pagination limit ($pageCount pages, ${allNodes.size} results)" }
                break
            }
        }

        return allNodes
            .filter { it.mergedAt != null }
            .map { it.toDomain() }
            .sortedBy { it.mergedAt }
    }

    private suspend fun handleHttpError(httpResponse: HttpResponse) {
        logger.error { "GitHub API error: HTTP ${httpResponse.status.value} ${httpResponse.status.description}" }
        val errorBody = httpResponse.bodyAsText()
        logger.error { "Response body: $errorBody" }

        try {
            val error = Json.decodeFromString<GitHubErrorDto>(errorBody)
            throw IllegalStateException("GitHub API error: ${error.message}")
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            throw IllegalStateException("GitHub API error: HTTP ${httpResponse.status.value}")
        }
    }

    private fun PullRequestNodeDto.toDomain(): PullRequest {
        val mergedAtInstant = Instant.parse(mergedAt!!)
        val mergedAtDateTime = LocalDateTime.ofInstant(mergedAtInstant, ZoneOffset.UTC)
        val cleanedDescription =
            body
                ?.take(MAX_DESCRIPTION_LENGTH)
                ?.replace(Regex("[\\r\\n]+"), " ")
                ?.trim()
                ?.ifBlank { null }

        return PullRequest(
            number = number,
            title = title,
            url = url,
            mergedAt = mergedAtDateTime,
            description = cleanedDescription,
        )
    }

    override fun close() {
        logger.debug { "Closing GitHub HTTP client" }
        client.close()
    }
}
