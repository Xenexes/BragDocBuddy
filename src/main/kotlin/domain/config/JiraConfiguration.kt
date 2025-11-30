package domain.config

data class JiraConfiguration(
    val enabled: Boolean,
    val url: String?,
    val email: String?,
    val apiToken: String?,
    val jqlTemplate: String = DEFAULT_JQL_TEMPLATE,
) {
    fun isConfigured(): Boolean =
        !url.isNullOrBlank() &&
            !email.isNullOrBlank() &&
            !apiToken.isNullOrBlank()

    companion object {
        const val DEFAULT_JQL_TEMPLATE =
            """
            (
                assignee = "{email}"
                OR
                "Engineer[User Picker (single user)]" = "{email}"
                OR
                assignee WAS "{email}" DURING ("{startDate}", "{endDate}")
            )
            AND status was "In Progress"
            AND statusCategory IN (Done)
            AND "Last Transition Occurred[Date]" >= "{startDate}"
            AND "Last Transition Occurred[Date]" <= "{endDate}"
            """
    }
}
