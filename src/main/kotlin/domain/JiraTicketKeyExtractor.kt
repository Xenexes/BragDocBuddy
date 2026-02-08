package domain

object JiraTicketKeyExtractor {
    private val JIRA_KEY_PATTERN = Regex("\\b([A-Z][A-Z_0-9]+-[1-9]\\d*)\\b")

    fun extractKeys(vararg texts: String?): Set<String> =
        texts
            .filterNotNull()
            .flatMap { JIRA_KEY_PATTERN.findAll(it).map { match -> match.groupValues[1] } }
            .toSet()
}
