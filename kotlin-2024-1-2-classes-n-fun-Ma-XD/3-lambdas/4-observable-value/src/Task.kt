import kotlin.properties.Delegates

typealias OnChange<T> = (T) -> Unit

interface Value<T> {
    var value: T
    fun observe(onChange: OnChange<T>): Cancellation
}

fun interface Cancellation {
    fun cancel()
}

class MutableValue<T>(initial: T) : Value<T> {
    // for saving order and constant-time add and remove
    private val onChangeList = mutableSetOf<OnChange<T>>()

    override var value: T by Delegates.observable(initial) { _, _, newValue ->
        onChangeList.forEach { it(newValue) }
    }

    override fun observe(onChange: OnChange<T>): Cancellation {
        onChangeList.add(onChange)
        onChange(value)
        return Cancellation { onChangeList.remove(onChange) }
    }
}
