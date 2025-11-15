package domain

enum class Timeframe {
    TODAY,
    YESTERDAY,
    LAST_WEEK,
    LAST_MONTH,
    LAST_YEAR,
    QUARTER_ONE,
    QUARTER_TWO,
    QUARTER_THREE,
    QUARTER_FOUR,
    ;

    companion object {
        fun fromString(value: String): Timeframe? =
            when (value.lowercase()) {
                "today" -> TODAY
                "yesterday" -> YESTERDAY
                "last-week", "lastweek" -> LAST_WEEK
                "last-month", "lastmonth" -> LAST_MONTH
                "last-year", "lastyear" -> LAST_YEAR
                "quarter-one", "quarterone", "q1" -> QUARTER_ONE
                "quarter-two", "quartertwo", "q2" -> QUARTER_TWO
                "quarter-three", "quarterthree", "q3" -> QUARTER_THREE
                "quarter-four", "quarterfour", "q4" -> QUARTER_FOUR
                else -> null
            }
    }
}
