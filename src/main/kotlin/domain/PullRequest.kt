package domain

import java.time.LocalDateTime

data class PullRequest(
    val number: Int,
    val title: String,
    val url: String,
    val mergedAt: LocalDateTime,
)
