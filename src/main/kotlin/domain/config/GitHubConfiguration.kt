package domain.config

data class GitHubConfiguration(
    val enabled: Boolean,
    val token: String?,
    val username: String?,
    val organization: String?,
) {
    fun isConfigured(): Boolean =
        !token.isNullOrBlank() &&
            !username.isNullOrBlank() &&
            !organization.isNullOrBlank()
}
