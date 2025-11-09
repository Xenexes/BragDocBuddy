package domain

enum class Timeframe {
    TODAY,
    YESTERDAY,
    LAST_WEEK,
    LAST_MONTH,
    LAST_YEAR,
    ;

    companion object {
        fun fromString(value: String): Timeframe? =
            when (value.lowercase()) {
                "today" -> TODAY
                "yesterday" -> YESTERDAY
                "last-week", "lastweek" -> LAST_WEEK
                "last-month", "lastmonth" -> LAST_MONTH
                "last-year", "lastyear" -> LAST_YEAR
                else -> null
            }
    }
}
