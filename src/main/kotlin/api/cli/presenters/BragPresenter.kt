package api.cli.presenters

import domain.BragEntry
import java.time.format.DateTimeFormatter

class BragPresenter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun present(bragsByDate: Map<String, List<BragEntry>>) {
        if (bragsByDate.isEmpty()) {
            println("No brags found in this time period.")
            return
        }

        println()
        bragsByDate.forEach { (date, entries) ->
            println(date)
            entries.forEach { entry ->
                val timeStr = entry.timestamp.format(timeFormatter)
                println("  * $timeStr ${entry.content}")
            }
            println()
        }
    }
}
