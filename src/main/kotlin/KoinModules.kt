import infrastructure.git.GitVersionControl
import infrastructure.git.NoOpVersionControl
import infrastructure.github.GitHubConfiguration
import infrastructure.github.KtorGitHubClient
import infrastructure.input.ConsoleUserInput
import infrastructure.jira.JiraConfiguration
import infrastructure.jira.JiraIssueFilter
import infrastructure.jira.JiraQueryBuilder
import infrastructure.jira.KtorJiraClient
import infrastructure.parser.TimeframeToDateRangeParser
import infrastructure.persistence.MarkdownBragRepository
import infrastructure.version.CurrentVersionProvider
import infrastructure.version.GitHubVersionChecker
import infrastructure.version.PropertiesVersionProvider
import infrastructure.version.VersionChecker
import org.koin.dsl.module
import ports.BragRepository
import ports.GitHubClient
import ports.JiraClient
import ports.TimeframeParser
import ports.UserInput
import ports.VersionControl
import usecases.AddBragUseCase
import usecases.GetBragsUseCase
import usecases.InitRepositoryUseCase
import usecases.SyncJiraIssuesUseCase
import usecases.SyncPullRequestsUseCase

val configurationModule =
    module {
        single<ApplicationConfiguration> {
            val docsLocation =
                System.getenv("BRAG_DOC")
                    ?: throw ConfigurationException("BRAG_DOC environment variable not set")

            val versionControlEnabled = System.getenv("BRAG_DOC_REPO_SYNC")?.lowercase() == "true"

            ApplicationConfiguration(
                docsLocation = docsLocation,
                versionControlEnabled = versionControlEnabled,
            )
        }

        single<GitHubConfiguration> {
            val enabled = System.getenv("BRAG_DOC_GITHUB_PR_SYNC_ENABLED")?.lowercase() != "false"

            val token = getGitHubToken() ?: System.getenv("BRAG_DOC_GITHUB_TOKEN")
            val username = System.getenv("BRAG_DOC_GITHUB_USERNAME")
            val organization = System.getenv("BRAG_DOC_GITHUB_ORG")

            GitHubConfiguration(
                enabled = enabled,
                token = token,
                username = username,
                organization = organization,
            )
        }

        single<JiraConfiguration> {
            val enabled = System.getenv("BRAG_DOC_JIRA_SYNC_ENABLED")?.lowercase() != "false"

            val url = System.getenv("BRAG_DOC_JIRA_URL")
            val email = System.getenv("BRAG_DOC_JIRA_EMAIL")
            val apiToken = System.getenv("BRAG_DOC_JIRA_API_TOKEN")
            val jqlTemplate = System.getenv("BRAG_DOC_JIRA_JQL_TEMPLATE")

            JiraConfiguration(
                enabled = enabled,
                url = url,
                email = email,
                apiToken = apiToken,
                jqlTemplate = jqlTemplate ?: JiraConfiguration.DEFAULT_JQL_TEMPLATE,
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

        single<CurrentVersionProvider> {
            PropertiesVersionProvider()
        }

        single<VersionChecker> {
            GitHubVersionChecker(
                currentVersionProvider = get(),
                githubRepository = "Xenexes/BragDocBuddy",
            )
        }

        single<GitHubClient> {
            KtorGitHubClient(get())
        }

        single<JiraClient> {
            KtorJiraClient(
                configuration = get(),
                queryBuilder = JiraQueryBuilder,
                issueFilter = JiraIssueFilter,
            )
        }

        single<UserInput> {
            ConsoleUserInput()
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

        single<SyncPullRequestsUseCase> {
            SyncPullRequestsUseCase(
                gitHubClient = get(),
                bragRepository = get(),
                timeframeParser = get(),
                gitHubConfig = get(),
            )
        }

        single<SyncJiraIssuesUseCase> {
            SyncJiraIssuesUseCase(
                jiraClient = get(),
                bragRepository = get(),
                timeframeParser = get(),
                jiraConfig = get(),
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

private fun getGitHubToken(): String? =
    try {
        val process = ProcessBuilder("gh", "auth", "token").start()
        val token =
            process.inputStream
                .bufferedReader()
                .readText()
                .trim()
        if (process.waitFor() == 0 && token.isNotEmpty()) token else null
    } catch (e: Exception) {
        null
    }
