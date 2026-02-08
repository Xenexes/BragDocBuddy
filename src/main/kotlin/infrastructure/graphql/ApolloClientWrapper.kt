package infrastructure.graphql

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import io.ktor.client.HttpClient

class ApolloClientWrapper(
    private val apolloClient: ApolloClient,
    private val httpClient: HttpClient,
) {
    suspend fun <T : Query.Data> query(query: Query<T>): ApolloResponse<T> = apolloClient.query(query).execute()

    fun close() {
        apolloClient.close()
        httpClient.close()
    }
}
