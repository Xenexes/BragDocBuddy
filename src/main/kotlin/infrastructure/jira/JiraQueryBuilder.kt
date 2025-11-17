package infrastructure.jira

import domain.DateRange

object JiraQueryBuilder {
    fun buildJQL(
        jqlTemplate: String,
        email: String,
        dateRange: DateRange,
    ): String =
        jqlTemplate
            .replace("{email}", email)
            .replace("{startDate}", dateRange.start.toString())
            .replace("{endDate}", dateRange.end.toString())
            .trimIndent()
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
