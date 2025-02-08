import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

typealias ImageGenerator = (query: String) -> ByteArray

class ImageProcessor(
    private val parallelism: Int,
    private val requests: ReceiveChannel<String>,
    private val publications: SendChannel<Pair<String, ByteArray>>,
    private val generator: ImageGenerator,
) {

    fun run(scope: CoroutineScope) {
        val cache = mutableSetOf<String>()
        val descriptions = Channel<String>(capacity = parallelism)

        scope.launch {
            for (description in requests) {
                if (cache.add(description)) {
                    descriptions.send(description)
                }
            }
            descriptions.close()
            cache.clear()
        }

        repeat(parallelism) {
            scope.launch {
                for (description in descriptions) {
                    val image = generator(description)
                    publications.send(description to image)
                }
            }
        }
    }
}
