import api.cli.BragCliApplication
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    try {
        startKoin {
            modules(appModules)
        }

        val app = BragCliApplication()
        app.run(args)
    } catch (e: ConfigurationException) {
        println("Configuration Error: ${e.message}")
        println("\nPlease ensure the following environment variables are set:")
        println("  BRAG_LOG            - Path to your brag documents directory (required)")
        println("  BRAG_LOG_REPO_SYNC  - Set to 'true' to enable git sync (optional)")
        exitProcess(1)
    } catch (e: Exception) {
        println("Error: ${e.message}")
        exitProcess(1)
    } finally {
        stopKoin()
    }
}

data class ApplicationConfiguration(
    val docsLocation: String,
    val versionControlEnabled: Boolean,
)
