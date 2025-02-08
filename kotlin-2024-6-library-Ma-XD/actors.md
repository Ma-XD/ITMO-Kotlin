# Actor model
Ознакомьтесь с [Моделью Акторов](https://ru.wikipedia.org/wiki/%D0%9C%D0%BE%D0%B4%D0%B5%D0%BB%D1%8C_%D0%B0%D0%BA%D1%82%D0%BE%D1%80%D0%BE%D0%B2).

## Мотивация

Предположим, нам необходимо разработать атомарный счетчик тип `Int`, который будет корректно поддерживать свое значение при параллельных операциях `+= 1`.

Самое простое решение - завести `AtomicInteger` переменную и воспользоваться ее методами `incrementAndGet()` и `get()`.
Эти методы гарантируют, что одновременный их вызов в нескольких потоках будут приводить к корректным значениям счетчика.
Такое решение работает в случае простого счетчика, но что если состояние нашего приложения - сложная комбинация нескольких структур данных?

Второе решение - завести `Mutex` и обычную изменяемую переменную `value` типа `Int`.
Будем изменять и получать значение переменной `value` только внутри критической секции Mutex (`mutex.withLock { ... }`).
Такое решение тоже будет давать корректные значения счетчика, так как java гарантирует видимость изменений при выходе и последующем входе в критическую секцию.

Mutex также хорошо применим, когда состояние нашего приложения сложнее чем просто число. Но давайте заметим следующий факт.
Использование Mutex делает так, что все изменения нашего приложения происходят _последовательно_.
А если все изменения происходят последовательно, то давайте выделаем специальный поток, в котором будем изменять наше состояние.

## Использование модели actor'ов

Для передачи запросов от счетчика к потоку-исполнителю заведем `MutableSharedFlow`.
Если запрос предполагает возвращаемое значение - ответ, вместе с запросом предадим CompletableDeferred - контейнер, в который нужно сохранить результат.
Все запросы представим в виде классов или объектов, наследующихся от некоторого общего sealed класса или интерфейса. 

Рассмотрим следующий пример.
```kotlin
sealed class CounterMsg // common sealed class for actor messages
data object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply

class Counter {
    private val updatesFlow = MutableSharedFlow<CounterMsg>()
    private val scope = CoroutineScope(newSingleThreadContext("CounterMsg"))

    init {
        scope.launch {
            var counter = 0
            updatesFlow.collect { msg ->
                when (msg) {
                    is IncCounter -> counter++
                    is GetCounter -> msg.response.complete(counter)
                }
            }
        }
    }

    suspend fun increment() {
        updatesFlow.emit(IncCounter)
    }

    suspend fun get(): Int {
        val response = CompletableDeferred<Int>()
        updatesFlow.emit(GetCounter(response))
        return response.await()
    }

    fun cancel() { scope.cancel() }
}


suspend fun main() {
    val counter = Counter()

    withContext(Dispatchers.Default) {
        for (i in 0..<1000000) {
            launch { counter.increment() }
        }
    }
    // send a message to get a counter value from an actor
    val result = counter.get()
    println("Counter = $result")
    counter.cancel() // shutdown the actor
}
```

Понятно, что для простого счетчика пример получился слишком громоздким.
Однако в некоторых случаях, модель акторов может быть довольно удобна.

Предположим, мы захотели добавить в счетчик операцию возведения текущего значения в степень `x`.
Обычные `AtomicInteger` такой операции не имеют. При этом в текущий пример такую операцию очень легко добавить, определив дополнительное сообщение и добавив для него соответствующий обработчик.
```kotlin
class PowCounter(val x: Int) : CounterMsg()
```

Важный момент, на который стоит обратить внимание.
Необходимо следить за тем, какие объекты (изменяемые или неизменяемые) мы храним в качестве состояния в actor model.
Важно, чтобы объект, который мы вернули в качестве результата выполнения операции, не изменился в последствии из-за последующих изменений состояния.