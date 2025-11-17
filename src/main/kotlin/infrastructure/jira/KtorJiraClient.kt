package infrastructure.jira

import domain.DateRange
import domain.JiraIssue
import infrastructure.jira.dto.JiraErrorDto
import infrastructure.jira.dto.JiraIssueDto
import infrastructure.jira.dto.JiraSearchJqlResponseDto
import infrastructure.jira.dto.JiraUserDto
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ports.JiraClient
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64

private val logger = KotlinLogging.logger {}

// Jira uses ISO-8601 format but without colon in timezone offset (e.g., +0100 instead of +01:00)
private val jiraDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

class KtorJiraClient(
    private val configuration: JiraConfiguration,
) : JiraClient {
    private val client =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }

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
        val maxResults = 50
        var nextPageToken: String? = null
        var isFirstRequest = true

        val jql = buildJQL(email, dateRange)

        logger.info { "Fetching resolved Jira issues for $email from ${dateRange.start} to ${dateRange.end}" }

        while (isFirstRequest || nextPageToken != null) {
            val httpResponse: HttpResponse =
                client.get("${configuration.url}/rest/api/3/search/jql") {
                    headers {
                        append(HttpHeaders.Authorization, authHeader)
                        append(HttpHeaders.ContentType, "application/json")
                    }
                    parameter("jql", jql)
                    parameter("maxResults", maxResults)
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
                    val matches = issue.isUserInvolvedDuringInProgress(email, userAccountId, dateRange)
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
            .map { it.toDomain(configuration.url!!) }
            .sortedBy { it.resolvedAt }
    }

    private fun buildJQL(
        email: String,
        dateRange: DateRange,
    ): String =
        configuration.jqlTemplate
            .replace("{email}", email)
            .replace("{startDate}", dateRange.start.toString())
            .replace("{endDate}", dateRange.end.toString())
            .trimIndent()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun JiraIssueDto.toDomain(jiraUrl: String): JiraIssue {
        val transitionDate = findDoneTransitionDate()

        val dateString =
            transitionDate
                ?: fields.resolutiondate
                ?: fields.updated

        val resolvedAtInstant = OffsetDateTime.parse(dateString, jiraDateTimeFormatter).toInstant()
        val resolvedAtDateTime = LocalDateTime.ofInstant(resolvedAtInstant, ZoneOffset.UTC)

        return JiraIssue(
            key = key,
            title = fields.summary,
            url = "$jiraUrl/browse/$key",
            resolvedAt = resolvedAtDateTime,
        )
    }

    private fun JiraIssueDto.findDoneTransitionDate(): String? {
        // Find the most recent transition where status changed
        // Look for transitions to the current status (which is in "Done" category based on JQL)
        val currentStatus = fields.status.name

        return changelog
            ?.histories
            ?.asReversed()
            ?.firstOrNull { history ->
                history.items.any { item ->
                    item.field == "status" && item.toString == currentStatus
                }
            }?.created
    }

    internal fun JiraIssueDto.isUserInvolvedDuringInProgress(
        email: String,
        userAccountId: String?,
        dateRange: DateRange,
    ): Boolean {
        if (fields.assignee?.emailAddress == email) {
            return true
        }

        if (fields.engineer?.emailAddress == email) {
            return true
        }

        // If we don't have the account ID, we can't check changelog
        if (userAccountId == null) {
            return false
        }

        // Check if user was assigned during an "In Progress" period within the date range
        val histories = changelog?.histories ?: return false

        // Build complete timeline of ALL changes (status and assignee)
        data class StateChange(
            val timestamp: OffsetDateTime,
            val statusChange: String?,
            val assigneeChange: String?,
        )

        val changes = mutableListOf<StateChange>()

        histories.sortedBy { it.created }.forEach { history ->
            val timestamp = OffsetDateTime.parse(history.created, jiraDateTimeFormatter)

            var statusChange: String? = null
            var assigneeChange: String? = null

            history.items.forEach { item ->
                when (item.field) {
                    "status" -> statusChange = item.toString
                    "assignee" -> assigneeChange = item.to
                }
            }

            if (statusChange != null || assigneeChange != null) {
                changes.add(StateChange(timestamp, statusChange, assigneeChange))
            }
        }

        // Track current state and find periods where BOTH status="In Progress" AND assignee=user
        var currentStatus: String? = null
        var currentAccountId: String? = null
        var userInProgressStartedAt: OffsetDateTime? = null

        changes.sortedBy { it.timestamp }.forEach { change ->
            val previousStatus = currentStatus
            val previousAccountId = currentAccountId

            if (change.statusChange != null) {
                currentStatus = change.statusChange
            }
            if (change.assigneeChange != null) {
                currentAccountId = change.assigneeChange
            }

            // Check if we entered a "user assigned + in progress" period
            val wasUserInProgress =
                previousStatus.equals("In Progress", true) &&
                    previousAccountId == userAccountId

            val isUserInProgress =
                currentStatus.equals("In Progress", true) &&
                    currentAccountId == userAccountId

            // Started a qualifying period
            if (!wasUserInProgress && isUserInProgress) {
                userInProgressStartedAt = change.timestamp
            }

            // Ended a qualifying period
            if (wasUserInProgress && !isUserInProgress && userInProgressStartedAt != null) {
                val periodStart = userInProgressStartedAt.toLocalDate()
                val periodEnd = change.timestamp.toLocalDate()

                // Check if this period overlaps with the date range
                if (periodStart <= dateRange.end && periodEnd >= dateRange.start) {
                    return true
                }

                userInProgressStartedAt = null
            }
        }

        // Check if we're still in a qualifying period
        if (currentStatus.equals("In Progress", true) &&
            currentAccountId == userAccountId &&
            userInProgressStartedAt != null
        ) {
            val periodStart = userInProgressStartedAt.toLocalDate()
            // Period is ongoing, check if it overlaps with date range
            if (periodStart <= dateRange.end) {
                return true
            }
        }

        return false
    }
}
