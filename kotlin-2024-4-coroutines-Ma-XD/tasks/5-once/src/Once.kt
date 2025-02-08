import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Once {
    private val mutex = Mutex()
    private var ran = false

    fun run(block: () -> Unit) = runBlocking {
        mutex.withLock {
            if (!ran) {
                ran = true
            } else {
                return@runBlocking
            }
        }
        block()
    }
}
