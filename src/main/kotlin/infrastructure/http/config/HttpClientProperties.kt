package infrastructure.http.config

import java.time.Duration

data class HttpClientProperties(
    val callTimeout: Duration = Duration.ofSeconds(30),
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val retry: RetryProperties? = RetryProperties(),
)

data class RetryProperties(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = Duration.ofSeconds(1),
    val multiplier: Double = 2.0,
    val maxDelay: Duration = Duration.ofMinutes(1),
    val exponentialBackoff: Boolean = true,
    val retryOnTimeout: Boolean = true,
)
