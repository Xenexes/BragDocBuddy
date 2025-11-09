import infrastructure.git.GitVersionControl
import infrastructure.git.NoOpVersionControl
import infrastructure.parser.TimeframeToDateRangeParser
import infrastructure.persistence.MarkdownBragRepository
import org.koin.dsl.module
import ports.BragRepository
import ports.TimeframeParser
import ports.VersionControl
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase

val configurationModule =
    module {
        single<ApplicationConfiguration> {
            val docsLocation =
                System.getenv("BRAG_LOC")
                    ?: throw ConfigurationException("BRAG_LOC environment variable not set")

            val versionControlEnabled = System.getenv("BRAG_LOC_REPO_SYNC")?.lowercase() != "false"

            ApplicationConfiguration(
                docsLocation = docsLocation,
                versionControlEnabled = versionControlEnabled,
            )
        }
    }

val infrastructureModule =
    module {
        single<BragRepository> {
            MarkdownBragRepository(get<ApplicationConfiguration>().docsLocation)
        }

        single<VersionControl> {
            val config = get<ApplicationConfiguration>()
            if (config.versionControlEnabled) {
                GitVersionControl(config.docsLocation)
            } else {
                NoOpVersionControl()
            }
        }

        single<TimeframeParser> {
            TimeframeToDateRangeParser()
        }
    }

val useCaseModule =
    module {
        single<InitRepositoryUseCase> {
            InitRepositoryUseCase(get())
        }

        single<AddBragUseCase> {
            AddBragUseCase(
                repository = get(),
                versionControl = get(),
                docsLocation = get<ApplicationConfiguration>().docsLocation,
            )
        }

        single<GetBragsUseCase> {
            GetBragsUseCase(
                repository = get(),
                timeframeParser = get(),
            )
        }
    }

val appModules =
    listOf(
        configurationModule,
        infrastructureModule,
        useCaseModule,
    )

class ConfigurationException(
    message: String,
) : Exception(message)
