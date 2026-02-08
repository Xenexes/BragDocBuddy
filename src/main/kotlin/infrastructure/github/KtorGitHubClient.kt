package infrastructure.github

import domain.DateRange
import domain.PullRequest
import domain.config.GitHubConfiguration
import infrastructure.github.graphql.SearchPullRequestsQuery
import infrastructure.graphql.GraphqlClientFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import ports.GitHubClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

private val logger = KotlinLogging.logger {}

class KtorGitHubClient(
    private val configuration: GitHubConfiguration,
) : GitHubClient,
    AutoCloseable {
    companion object {
        private const val GITHUB_API_MAX_PAGES = 10
        private const val GITHUB_GRAPHQL_URL = "https://api.github.com/graphql"
        private const val MAX_DESCRIPTION_LENGTH = 2500
    }

    private val graphqlClient =
        GraphqlClientFactory.create(
            serverUrl = GITHUB_GRAPHQL_URL,
            tokenProvider = {
                configuration.token ?: throw IllegalStateException("GitHub token must not be null")
            },
        )

    override suspend fun fetchMergedPullRequests(
        organization: String,
        author: String,
        dateRange: DateRange,
    ): List<PullRequest> {
        val allPullRequests = mutableListOf<SearchPullRequestsQuery.OnPullRequest>()
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

            val response =
                graphqlClient.query(
                    SearchPullRequestsQuery(
                        query = searchQuery,
                        cursor = GraphqlClientFactory.optionalCursor(cursor),
                    ),
                )

            response.exception?.let { exception ->
                logger.error(exception) { "GraphQL request failed" }
                throw IllegalStateException("GitHub GraphQL request failed: ${exception.message}", exception)
            }

            if (!response.errors.isNullOrEmpty()) {
                val errorMessages = response.errors!!.joinToString("; ") { it.message }
                logger.error { "GraphQL errors: $errorMessages" }
                throw IllegalStateException("GitHub GraphQL error: $errorMessages")
            }

            val searchResult =
                response.data?.search
                    ?: throw IllegalStateException("GitHub GraphQL error: no data in response")

            val nodes =
                searchResult.nodes
                    ?.mapNotNull { it?.onPullRequest }
                    ?: emptyList()

            logger.info { "Page $pageCount: Found ${nodes.size} PRs" }

            allPullRequests.addAll(nodes)

            if (!searchResult.pageInfo.hasNextPage) break

            cursor = searchResult.pageInfo.endCursor

            if (pageCount >= GITHUB_API_MAX_PAGES) {
                logger.warn { "Reached API pagination limit ($pageCount pages, ${allPullRequests.size} results)" }
                break
            }
        }

        return allPullRequests
            .filter { it.mergedAt != null }
            .map { it.toDomain() }
            .sortedBy { it.mergedAt }
    }

    private fun SearchPullRequestsQuery.OnPullRequest.toDomain(): PullRequest {
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
        graphqlClient.close()
    }
}
