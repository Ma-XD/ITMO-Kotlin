import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

class ParallelEvaluator {
    suspend fun run(task: Task, n: Int, context: CoroutineContext) = withContext(context) {
        try {
            runParallel(task, n)
        } catch (e: Exception) {
            throw TaskEvaluationException(e)
        }
    }

    private suspend fun runParallel(task: Task, n: Int) = coroutineScope {
        repeat(n) { i ->
            launch { task.run(i) }
        }
    }
}
