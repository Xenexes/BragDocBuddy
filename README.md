Brag - A command line tool for journaling daily accomplishments
Inspired by [Julia Evans' blog post on brag documents](https://jvns.ca/blog/brag-documents/)

Usage:
    brag init                              Initialize bragging document directory
    brag -c "YOUR TEXT HERE"               Add a new brag entry
    brag --comment "YOUR TEXT HERE"        Add a new brag entry
    brag about <timeframe>                 Review brags from a time period
    
Timeframes:
    today, yesterday, last-week, last-month, last-year
    
Environment Variables:
    BRAG_DOCS_LOC       Location of bragging document directory
    BRAG_DOCS_REPO_SYNC Set to 'true' to automatically commit and push to git