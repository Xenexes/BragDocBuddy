package infrastructure.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.network.ktorClient
import infrastructure.http.HttpClientFactory
import infrastructure.http.config.HttpClientProperties
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

object GraphqlClientFactory {
    fun create(
        serverUrl: String,
        tokenProvider: () -> String,
        httpClientProperties: HttpClientProperties = HttpClientProperties(),
    ): ApolloClientWrapper {
        val httpClient =
            HttpClientFactory.create(serverUrl, httpClientProperties).config {
                install(DefaultRequest) {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider()}")
                }
            }

        val apolloClient =
            ApolloClient
                .Builder()
                .serverUrl(serverUrl)
                .ktorClient(httpClient)
                .build()

        return ApolloClientWrapper(apolloClient, httpClient)
    }

    fun optionalCursor(cursor: String?): Optional<String> =
        cursor?.let { Optional.Present(it) }
            ?: Optional.Absent
}
