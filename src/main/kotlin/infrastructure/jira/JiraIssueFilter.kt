package infrastructure.jira

import domain.DateRange
import infrastructure.jira.dto.JiraIssueDto
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object JiraIssueFilter {
    private val jiraDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

    fun isUserInvolvedDuringInProgress(
        issue: JiraIssueDto,
        email: String,
        userAccountId: String?,
        dateRange: DateRange,
    ): Boolean {
        if (issue.fields.assignee?.emailAddress == email) {
            return true
        }

        if (issue.fields.engineer?.emailAddress == email) {
            return true
        }

        // If we don't have the account ID, we can't check changelog
        if (userAccountId == null) {
            return false
        }

        // Check if user was assigned during an "In Progress" period within the date range
        val histories = issue.changelog?.histories ?: return false

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
