package infrastructure.jira

import domain.DateRange
import domain.JiraIssue
import domain.config.JiraConfiguration
import infrastructure.http.HttpClientFactory
import infrastructure.jira.dto.JiraErrorDto
import infrastructure.jira.dto.JiraIssueDto
import infrastructure.jira.dto.JiraSearchJqlResponseDto
import infrastructure.jira.dto.JiraUserDto
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import ports.JiraClient
import java.util.Base64

private val logger = KotlinLogging.logger {}

class KtorJiraClient(
    httpClientFactory: HttpClientFactory,
    private val configuration: JiraConfiguration,
    private val queryBuilder: JiraQueryBuilder,
    private val issueFilter: JiraIssueFilter,
) : JiraClient,
    AutoCloseable {
    companion object {
        private const val JIRA_API_PAGE_SIZE = 50
    }

    private val client = httpClientFactory.create()

    private suspend fun getUserAccountId(
        email: String,
        authHeader: String,
    ): String? {
        try {
            val response: HttpResponse =
                client.get("${configuration.url}/rest/api/3/user/search") {
                    headers {
                        append(HttpHeaders.Authorization, authHeader)
                        append(HttpHeaders.ContentType, "application/json")
                    }
                    parameter("query", email)
                }

            if (response.status.value in 200..299) {
                val users: List<JiraUserDto> = response.body()
                return users.firstOrNull { it.emailAddress == email }?.accountId
            } else {
                logger.warn { "Failed to fetch user account ID: HTTP ${response.status.value}" }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error fetching user account ID for $email" }
        }
        return null
    }

    override suspend fun fetchResolvedIssues(
        email: String,
        dateRange: DateRange,
    ): List<JiraIssue> {
        val authHeader =
            "Basic ${
                Base64.getEncoder().encodeToString(
                    "${configuration.email}:${configuration.apiToken}".toByteArray(),
                )
            }"

        // First, get the user's account ID from their email
        val userAccountId = getUserAccountId(email, authHeader)
        logger.info { "User account ID for $email: $userAccountId" }

        val allIssues = mutableListOf<JiraIssueDto>()
        var nextPageToken: String? = null
        var isFirstRequest = true

        val jql = queryBuilder.buildJQL(configuration.jqlTemplate, email, dateRange)

        logger.info { "Fetching resolved Jira issues for $email from ${dateRange.start} to ${dateRange.end}" }

        while (isFirstRequest || nextPageToken != null) {
            val httpResponse: HttpResponse =
                client.get("${configuration.url}/rest/api/3/search/jql") {
                    headers {
                        append(HttpHeaders.Authorization, authHeader)
                        append(HttpHeaders.ContentType, "application/json")
                    }
                    parameter("jql", jql)
                    parameter("maxResults", JIRA_API_PAGE_SIZE)
                    parameter("fields", "*navigable")
                    parameter("expand", "changelog")

                    if (nextPageToken != null) {
                        parameter("nextPageToken", nextPageToken)
                    }
                }

            if (httpResponse.status.value !in 200..299) {
                logger.error { "Jira API error: HTTP ${httpResponse.status.value} ${httpResponse.status.description}" }
                val errorBody = httpResponse.bodyAsText()
                logger.error { "Response body: $errorBody" }

                val errorMessage =
                    try {
                        val error = Json.decodeFromString<JiraErrorDto>(errorBody)
                        error.errorMessages?.joinToString(", ")
                            ?: error.errors?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
                            ?: "Unknown error"
                    } catch (e: Exception) {
                        "HTTP ${httpResponse.status.value} - Could not parse error response"
                    }

                throw IllegalStateException("Jira API error: $errorMessage")
            }

            val response: JiraSearchJqlResponseDto = httpResponse.body()

            logger.info { "Fetched ${response.issues.size} issues (Total so far: ${allIssues.size + response.issues.size})" }

            if (response.issues.isEmpty()) break

            allIssues.addAll(response.issues)

            nextPageToken = response.nextPageToken
            isFirstRequest = false
        }

        logger.info { "Total issues fetched from Jira: ${allIssues.size}" }

        // Only apply client-side filtering if using the default JQL template
        val isUsingDefaultTemplate = configuration.jqlTemplate == JiraConfiguration.DEFAULT_JQL_TEMPLATE
        logger.info { "Using default JQL template: $isUsingDefaultTemplate" }

        val filtered =
            if (isUsingDefaultTemplate) {
                allIssues.filter { issue ->
                    val matches = issueFilter.isUserInvolvedDuringInProgress(issue, email, userAccountId, dateRange)
                    if (!matches) {
                        logger.info {
                            "Filtered out: [${issue.key}] ${issue.fields.summary}\n" +
                                "  ${configuration.url}/browse/${issue.key}"
                        }
                    }
                    matches
                }
            } else {
                logger.info { "Using custom JQL template - skipping client-side filtering" }
                allIssues
            }

        logger.info { "Issues after filtering: ${filtered.size}" }

        return filtered
            .map { JiraIssueDomainMapper.toDomain(it, configuration.url!!) }
            .sortedBy { it.resolvedAt }
    }

    override fun close() {
        logger.debug { "Closing Jira HTTP client" }
        client.close()
    }
}
