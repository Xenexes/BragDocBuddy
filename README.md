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
* Automatically sync merged GitHub pull requests to your brag document

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

## Environment Variables

| Variable                              | Description                                                  | Default | Required |
|---------------------------------------|--------------------------------------------------------------|---------|----------|
| `BRAG_DOC`                            | Location of bragging document directory                      | -       | Yes      |
| `BRAG_DOC_REPO_SYNC`                  | Set to 'true' to automatically commit and push to git        | `false` | No       |
| `BRAG_DOC_GITHUB_PR_SYNC_ENABLED`     | Enable GitHub PR sync feature                                | `true`  | No       |
| `BRAG_DOC_GITHUB_TOKEN`               | GitHub personal access token (or use `gh auth login`)        | -       | No*      |
| `BRAG_DOC_GITHUB_USERNAME`            | Your GitHub username                                         | -       | No*      |
| `BRAG_DOC_GITHUB_ORG`                 | GitHub organization to search for PRs                        | -       | No*      |

\* Required when `BRAG_DOC_GITHUB_PR_SYNC_ENABLED=true`
