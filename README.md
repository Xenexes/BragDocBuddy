# BragLog Buddy

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

## References:

Inspired by [Julia Evans' blog post on brag documents](https://jvns.ca/blog/brag-documents/)

## Download/Update

```shell
curl -L https://github.com/Xenexes/BragLogBuddy/releases/download/v1.0.0/BragLogBuddy-macos > /Applications
```

## Setup

### Initialize

If you start a new brag doc, create a directory of your choice where to store the brag doc document. Initialize the git repo inside with `git init`.
If you have an existing brag doc, clone the existing repository. Set the environment variable `BRAG_LOG` to your brag doc directory.

```shell
export BRAG_LOG=/MY_BRAG_DOC_DIR
```

**Optional Git Sync:**
If you want to automatically commit and push your brags to git after each entry:
```shell
export BRAG_LOG_REPO_SYNC=true
```

On a new brag doc you can then initialize the document directory by using the `init` subcommand.

```shell
BragLogBuddy init
```

You can now start adding brag entries with the `-c` subcommand:

```shell
BragLogBuddy -c "YOUR NEW BRAG LOG ENTRY"
```

## Command overview

Initialize bragging document directory
```shell
BragLogBuddy init
```

Add a new brag entry
```shell
BragLogBuddy -c "YOUR NEW BRAG LOG ENTRY"
```     

Add a new brag entry
```shell
BragLogBuddy --comment "YOUR NEW BRAG LOG ENTRY"
```

Review brags from a time period
```shell
BragLogBuddy about <timeframe>
# Example: BragLogBuddy about last-week
```

Show current version and check for updates
```shell
BragLogBuddy version
```

## Timeframes
* `today`
* `yesterday`
* `last-week`
* `last-month`
* `last-year`

## Environment Variables

| Variable                    | Description                                                  | Default                         | Required |
|-----------------------------|--------------------------------------------------------------|---------------------------------|----------|
| `BRAG_LOG`                  | Location of bragging document directory                      | -                               | Yes      |
| `BRAG_LOG_REPO_SYNC`        | Set to 'true' to automatically commit and push to git        | `false`                         | No       |
