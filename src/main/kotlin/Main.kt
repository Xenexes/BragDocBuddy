import api.cli.BragCliApplication
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.java.KoinJavaComponent.getKoin
import ports.GitHubClient
import ports.JiraClient
import ports.VersionChecker
import kotlin.system.exitProcess

fun cleanupResources() {
    try {
        val koin = getKoin()
        // Close all AutoCloseable resources
        listOf(
            koin.getOrNull<JiraClient>(),
            koin.getOrNull<GitHubClient>(),
            koin.getOrNull<VersionChecker>(),
        ).forEach { resource ->
            (resource as? AutoCloseable)?.close()
        }
    } catch (e: Exception) {
        // Ignore errors during cleanup
    }
}

fun main(args: Array<String>) {
    // Register shutdown hook to ensure resources are cleaned up
    Runtime.getRuntime().addShutdownHook(
        Thread {
            try {
                cleanupResources()
                stopKoin()
            } catch (e: Exception) {
                // Ignore errors during shutdown
            }
        },
    )

    try {
        startKoin {
            modules(appModules)
        }

        val app = BragCliApplication()
        app.run(args)
    } catch (e: ConfigurationException) {
        println("Configuration Error: ${e.message}")
        println("\nPlease ensure the following environment variables are set:")
        println("  BRAG_DOC            - Path to your brag documents directory (required)")
        println("  BRAG_DOC_REPO_SYNC  - Set to 'true' to enable git sync (optional)")
        exitProcess(1)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        exitProcess(1)
    } finally {
        cleanupResources()
        stopKoin()
    }
}

data class ApplicationConfiguration(
    val docsLocation: String,
    val versionControlEnabled: Boolean,
)
