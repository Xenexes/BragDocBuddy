package infrastructure.http

import infrastructure.http.config.HttpClientProperties
import infrastructure.http.config.RetryProperties
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    fun create(
        url: String,
        httpClientProperties: HttpClientProperties,
        additionalConfig: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient =
        create(url, httpClientProperties, OkHttp) {
            engine {
                config {
                    connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
                    dispatcher(
                        Dispatcher(
                            Executors.newCachedThreadPool { runnable ->
                                Thread(runnable).apply { isDaemon = true }
                            },
                        ),
                    )
                }
            }
            additionalConfig()
        }

    fun <T : HttpClientEngineConfig> create(
        url: String,
        httpClientProperties: HttpClientProperties,
        engineFactory: HttpClientEngineFactory<T>,
        additionalConfig: HttpClientConfig<T>.() -> Unit = {},
    ): HttpClient =
        HttpClient(engineFactory) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }

            install(Logging) {
                level = LogLevel.INFO
            }

            install(HttpTimeout) {
                requestTimeoutMillis = httpClientProperties.callTimeout.toMillis()
                connectTimeoutMillis = httpClientProperties.connectTimeout.toMillis()
                socketTimeoutMillis = httpClientProperties.readTimeout.toMillis()
            }

            httpClientProperties.retry
                ?.let { retryConfig -> addRetryConfig(this, retryConfig) }

            defaultRequest {
                contentType(ContentType.Application.Json)
                url(url)
            }

            additionalConfig()
        }

    private fun addRetryConfig(
        config: HttpClientConfig<*>,
        retryConfig: RetryProperties,
    ) {
        config.install(HttpRequestRetry) {
            maxRetries = retryConfig.maxAttempts
            retryOnException(maxRetries = retryConfig.maxAttempts, retryOnTimeout = retryConfig.retryOnTimeout)

            if (retryConfig.exponentialBackoff) {
                exponentialDelay(
                    base = retryConfig.multiplier,
                    maxDelayMs = retryConfig.maxDelay.toMillis(),
                )
            } else {
                constantDelay(millis = retryConfig.initialDelay.toMillis())
            }

            retryIf { _, response ->
                isServerError(response) ||
                    response.status == HttpStatusCode.TooManyRequests ||
                    response.status == HttpStatusCode.RequestTimeout
            }
        }
    }

    private fun isServerError(response: HttpResponse): Boolean = response.status.value in 500..599
}
