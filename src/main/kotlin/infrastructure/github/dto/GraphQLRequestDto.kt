package infrastructure.github.dto

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLRequestDto(
    val query: String,
    val variables: Map<String, String?>,
)
