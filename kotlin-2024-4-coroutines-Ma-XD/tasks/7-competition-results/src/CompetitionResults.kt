import kotlin.time.Duration
import kotlinx.coroutines.flow.*

fun Flow<Cutoff>.resultsFlow(): Flow<Results> =
    runningFold(mapOf<Int, Duration>()) { results, cutoff ->
        results + mapOf(cutoff.number to cutoff.time)
    }
        .drop(1) // drop initial
        .map { Results(it) }

fun Flow<Results>.scoreboard(): Flow<Scoreboard> = map {
    Scoreboard(
        it.results
            .toList()
            .sortedBy { (_, time) -> time }
            .mapIndexed { i, (number, time) ->
                ScoreboardRow(
                    rank = i + 1,
                    number = number,
                    time = time,
                )
            },
    )
}
