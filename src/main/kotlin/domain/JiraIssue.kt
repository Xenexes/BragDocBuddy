package domain

import java.time.LocalDateTime

data class JiraIssue(
    val key: String,
    val title: String,
    val url: String,
    val resolvedAt: LocalDateTime,
)
