# BragDocBuddy

A CLI tool for maintaining a "brag doc document" - your personal record of professional accomplishments.

## The core problem it solves:

Both you and your manager forget what you've achieved over time, making performance reviews difficult and potentially costing you recognition, promotions, or raises. By regularly documenting your work, you create a reliable record that helps with performance reviews, manager transitions, and career reflection.

## When writing entries, remember to:

* Capture everything, even small wins you think you'll remember (you won't)
* Include "fuzzy work" like mentoring, process improvements, and code quality efforts that often go unrecognized
* Focus on impact and results, not just activities (e.g., "reduced build time by 40%, saving team 2 hours daily" vs "improved build process").
* Document the "why" behind your work to show the bigger picture
* Record what you learned and skills you're developing

Don't oversell or undersell - just make your work sound exactly as good as it is.

## Why this tool

* I don't want to leave my IDE
* I always have multiple terminals open, so adding a new brag is super easy
* It's automatically versioned without thinking about it
* I can quickly get an overview of the last week, month, etc.
* I can use the overview to generate a summary or formated document with AI
* I can use the overview for self-assessment
* Automatically sync merged GitHub pull requests to your brag document
* Automatically sync your resolved Jira issues to your brag document

## References:

Inspired by [Julia Evans' blog post on brag documents](https://jvns.ca/blog/brag-documents/)

## Download/Update

```shell
curl -L https://github.com/Xenexes/BragDocBuddy/releases/download/v1.0.0/BragDocBuddy-macos > /Applications
```

## Setup

### Initialize

If you start a new brag doc, create a directory of your choice where to store the brag doc document. Initialize the git repo inside with `git init`.
If you have an existing brag doc, clone the existing repository. Set the environment variable `BRAG_DOC` to your brag doc directory.

```shell
export BRAG_DOC=/MY_BRAG_DOC_DIR
```

**Optional Git Sync:**
If you want to automatically commit and push your brags to git after each entry:
```shell
export BRAG_DOC_REPO_SYNC=true
```

**Optional GitHub PR Sync:**
If you want to automatically sync your merged GitHub pull requests to your brag document:
```shell
export BRAG_DOC_GITHUB_PR_SYNC_ENABLED=true
export BRAG_DOC_GITHUB_TOKEN=your_github_token  # or use 'gh auth login'
export BRAG_DOC_GITHUB_USERNAME=your_username
export BRAG_DOC_GITHUB_ORG=your_organization
```

**Optional Jira Issue Sync:**
If you want to automatically sync your resolved Jira issues to your brag document:
```shell
export BRAG_DOC_JIRA_SYNC_ENABLED=true
export BRAG_DOC_JIRA_URL=https://your-company.atlassian.net
export BRAG_DOC_JIRA_EMAIL=your.email@company.com
export BRAG_DOC_JIRA_API_TOKEN=your_api_token
```

**Custom Jira JQL Template (Optional):**
By default, BragDocBuddy uses a JQL query that fetches issues where you are currently assigned, where you're listed in the "Engineer" field, or where you were assigned during the timeframe, with "In Progress" status and "Done" status category.

If your Jira setup differs, you can customize the JQL query using placeholders:
```shell
export BRAG_DOC_JIRA_JQL_TEMPLATE='assignee = "{email}" AND statusCategory IN (Done) AND "Last Transition Occurred[Date]" >= "{startDate}" AND "Last Transition Occurred[Date]" <= "{endDate}"'
```

Available placeholders:
- `{email}` - Your Jira email address
- `{startDate}` - Start date of the timeframe (YYYY-MM-DD format)
- `{endDate}` - End date of the timeframe (YYYY-MM-DD format)

**How to get Jira API Token:**
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Give it a name (e.g., "BragDocBuddy")
4. Copy the token and set it as environment variable

On a new brag doc you can then initialize the document directory by using the `init` subcommand.

```shell
BragDocBuddy init
```

You can now start adding brag entries with the `-c` subcommand:

```shell
BragDocBuddy -c "YOUR NEW BRAG LOG ENTRY"
```

## Command overview

Initialize bragging document directory
```shell
BragDocBuddy init
```

Add a new brag entry
```shell
BragDocBuddy -c "YOUR NEW BRAG LOG ENTRY"
```     

Add a new brag entry
```shell
BragDocBuddy --comment "YOUR NEW BRAG LOG ENTRY"
```

Review brags from a time period
```shell
BragDocBuddy about <timeframe>
# Example: BragDocBuddy about last-week
```

Sync merged GitHub pull requests to brag document
```shell
BragDocBuddy sync-prs <timeframe>
# Example: BragDocBuddy sync-prs last-month
```

Preview GitHub pull requests without adding to brag document
```shell
BragDocBuddy sync-prs <timeframe> --print-only
# Example: BragDocBuddy sync-prs last-week --print-only
```

Sync resolved Jira issues to brag document
```shell
BragDocBuddy sync-jira <timeframe>
# Example: BragDocBuddy sync-jira quarter-four
```

Preview Jira issues without adding to brag document
```shell
BragDocBuddy sync-jira <timeframe> --print-only
# Example: BragDocBuddy sync-jira last-month --print-only
```

Show current version and check for updates
```shell
BragDocBuddy version
```

## Timeframes
* `today`
* `yesterday`
* `last-week`
* `last-month`
* `last-year`
* `q1` (January - March)
* `q2` (April - June)
* `q3` (July - September)
* `q4` (October - December)

## GitHub PR Sync

The GitHub PR sync feature automatically retrieves your merged pull requests and adds them to your brag document.

**Key Features:**
* Pull requests are inserted chronologically based on their **merge date/time**
* PRs are placed in the correct position among existing manual entries
* Date sections are automatically created for days with gaps
* Each PR entry includes: PR number, title, and URL
* **Duplicate detection**: Running the sync multiple times won't create duplicate entries

**Example:**
If your brag document has entries on Nov 1 and Nov 5, and you sync PRs merged on Nov 1 (12:00), Nov 3 (14:00), and Nov 5 (08:00):
- The Nov 1 PR will be inserted at 12:00 on Nov 1 (among other Nov 1 entries)
- A new date section will be created for Nov 3 with the PR at 14:00
- The Nov 5 PR will be inserted at 08:00 on Nov 5 (among other Nov 5 entries)

This ensures your brag document maintains a complete chronological timeline of your work.

## Jira Issue Sync

The Jira issue sync feature automatically retrieves your resolved Jira issues and adds them to your brag document.

**Key Features:**
* Issues are inserted chronologically based on their **resolution date/time**
* Issues are placed in the correct position among existing manual entries
* Each issue entry includes: Issue key, title, and URL
* **Interactive selection**: Review issues before adding them to your brag document
* **Duplicate detection**: Running the sync multiple times won't create duplicate entries

**Interactive Mode:**
When syncing Jira issues (without `--print-only`), you'll see all resolved issues and can choose which ones to skip:
```
Resolved Jira Issues:
================================================================================
[PROJ-123] Implement new feature
  https://company.atlassian.net/browse/PROJ-123
[PROJ-124] Fix critical bug
  https://company.atlassian.net/browse/PROJ-124
================================================================================

Enter issue keys to skip (comma-separated), or press Enter to add all:
> PROJ-124
```

This allows you to exclude issues that you don't want in your brag document (e.g., minor bug fixes, internal tasks).

**Example:**
```shell
# Review and interactively select issues to add
BragDocBuddy sync-jira quarter-four

# Just preview issues without adding
BragDocBuddy sync-jira last-month --print-only
```

## Environment Variables

| Variable                              | Description                                                  | Default | Required |
|---------------------------------------|--------------------------------------------------------------|---------|----------|
| `BRAG_DOC`                            | Location of bragging document directory                      | -       | Yes      |
| `BRAG_DOC_REPO_SYNC`                  | Set to 'true' to automatically commit and push to git        | `false` | No       |
| `BRAG_DOC_GITHUB_PR_SYNC_ENABLED`     | Enable GitHub PR sync feature                                | `true`  | No       |
| `BRAG_DOC_GITHUB_TOKEN`               | GitHub personal access token (or use `gh auth login`)        | -       | No*      |
| `BRAG_DOC_GITHUB_USERNAME`            | Your GitHub username                                         | -       | No*      |
| `BRAG_DOC_GITHUB_ORG`                 | GitHub organization to search for PRs                        | -       | No*      |
| `BRAG_DOC_JIRA_SYNC_ENABLED`          | Enable Jira issue sync feature                               | `true`  | No       |
| `BRAG_DOC_JIRA_URL`                   | Jira URL (e.g., https://your-company.atlassian.net)          | -       | No**     |
| `BRAG_DOC_JIRA_EMAIL`                 | Your Jira email address                                      | -       | No**     |
| `BRAG_DOC_JIRA_API_TOKEN`             | Jira API token                                               | -       | No**     |
| `BRAG_DOC_JIRA_JQL_TEMPLATE`          | Custom JQL query template with {email}, {startDate}, {endDate} placeholders | Built-in template | No       |

\* Required when `BRAG_DOC_GITHUB_PR_SYNC_ENABLED=true`
\*\* Required when `BRAG_DOC_JIRA_SYNC_ENABLED=true`
