package infrastructure.github.dto

import kotlinx.serialization.Serializable

@Serializable
data class GraphQLResponseDto(
    val data: GraphQLDataDto? = null,
    val errors: List<GraphQLErrorDto>? = null,
)

@Serializable
data class GraphQLDataDto(
    val search: GraphQLSearchDto,
)

@Serializable
data class GraphQLSearchDto(
    val pageInfo: PageInfoDto,
    val nodes: List<PullRequestNodeDto?>,
)

@Serializable
data class PageInfoDto(
    val hasNextPage: Boolean,
    val endCursor: String? = null,
)

@Serializable
data class PullRequestNodeDto(
    val number: Int,
    val title: String,
    val url: String,
    val mergedAt: String? = null,
    val body: String? = null,
)

@Serializable
data class GraphQLErrorDto(
    val message: String,
    val type: String? = null,
)
