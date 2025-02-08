import kotlinx.coroutines.*

class SequentialProcessor(private val handler: (String) -> String) : TaskProcessor {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val context = newSingleThreadContext("SequentialProcessor")

    override suspend fun process(argument: String): String = withContext(context) {
        handler(argument)
    }

    override fun close() {
        context.close()
    }
}
