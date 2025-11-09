package infrastructure.version

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Properties

private val logger = KotlinLogging.logger {}

interface VersionChecker {
    fun checkForUpdates()
}

class GitHubVersionChecker(
    private val currentVersionProvider: CurrentVersionProvider,
    private val githubRepository: String = "Xenexes/BragDocBuddy",
) : VersionChecker {
    companion object {
        private const val NOTIFICATION_BOX_WIDTH = 60
    }

    private val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()

    override fun checkForUpdates() {
        try {
            val currentVersion = currentVersionProvider.getCurrentVersion()
            println("BragDocBuddy version: $currentVersion")
            println()

            val latestVersion = fetchLatestVersion()

            if (latestVersion != null) {
                if (isNewerVersion(currentVersion, latestVersion)) {
                    printUpdateNotification(currentVersion, latestVersion)
                } else {
                    println("✓ You are using the latest version")
                }
            } else {
                println("Unable to check for updates. Please visit:")
                println("https://github.com/$githubRepository/releases")
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to check for updates" }
            println("Unable to check for updates. Please visit:")
            println("https://github.com/$githubRepository/releases")
        }
    }

    private fun fetchLatestVersion(): String? {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("https://api.github.com/repos/$githubRepository/releases/latest"))
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                parseVersionFromResponse(response.body())
            } else {
                logger.debug { "GitHub API returned status ${response.statusCode()}" }
                null
            }
        } catch (e: IOException) {
            logger.debug(e) { "Failed to fetch latest version from GitHub" }
            null
        } catch (e: InterruptedException) {
            logger.debug(e) { "Version check was interrupted" }
            Thread.currentThread().interrupt()
            null
        }
    }

    private fun parseVersionFromResponse(responseBody: String): String? =
        try {
            // Simple JSON parsing - extract "tag_name" value
            // Example: "tag_name":"v1.0.0" -> 1.0.0
            val tagNamePattern = """"tag_name"\s*:\s*"v?([^"]+)"""".toRegex()
            tagNamePattern.find(responseBody)?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.debug(e) { "Failed to parse version from GitHub response" }
            null
        }

    private fun isNewerVersion(
        current: String,
        latest: String,
    ): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(currentParts.size, latestParts.size)
            val currentPadded = currentParts + List(maxLength - currentParts.size) { 0 }
            val latestPadded = latestParts + List(maxLength - latestParts.size) { 0 }

            for (i in 0 until maxLength) {
                if (latestPadded[i] > currentPadded[i]) return true
                if (latestPadded[i] < currentPadded[i]) return false
            }
            return false
        } catch (e: Exception) {
            logger.debug(e) { "Failed to compare versions: current=$current, latest=$latest" }
            return false
        }
    }

    private fun printUpdateNotification(
        currentVersion: String,
        latestVersion: String,
    ) {
        val downloadUrl = "https://github.com/$githubRepository/releases"

        println()
        println("╔${"═".repeat(NOTIFICATION_BOX_WIDTH)}╗")
        println("║${centerText("A new version of BragDocBuddy is available!", NOTIFICATION_BOX_WIDTH)}║")
        println("║${" ".repeat(NOTIFICATION_BOX_WIDTH)}║")
        println("║  Current version: $currentVersion${" ".repeat(NOTIFICATION_BOX_WIDTH - 20 - currentVersion.length)}║")
        println("║  Latest version:  $latestVersion${" ".repeat(NOTIFICATION_BOX_WIDTH - 20 - latestVersion.length)}║")
        println("║${" ".repeat(NOTIFICATION_BOX_WIDTH)}║")
        println("║  Download: $downloadUrl${" ".repeat(NOTIFICATION_BOX_WIDTH - 13 - downloadUrl.length)}║")
        println("╚${"═".repeat(NOTIFICATION_BOX_WIDTH)}╝")
        println()
    }

    private fun centerText(
        text: String,
        width: Int,
    ): String {
        val padding = (width - text.length) / 2
        val extraPadding = if ((width - text.length) % 2 != 0) 1 else 0
        return " ".repeat(padding) + text + " ".repeat(padding + extraPadding)
    }
}

interface CurrentVersionProvider {
    fun getCurrentVersion(): String
}

class PropertiesVersionProvider : CurrentVersionProvider {
    override fun getCurrentVersion(): String =
        try {
            val properties = Properties()
            val versionFile = this::class.java.classLoader.getResourceAsStream("version.properties")

            if (versionFile != null) {
                properties.load(versionFile)
                properties.getProperty("version", "unknown")
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to load version from properties" }
            "unknown"
        }
}

class NoOpVersionChecker : VersionChecker {
    override fun checkForUpdates() {
        // Do nothing - version checking disabled
    }
}
