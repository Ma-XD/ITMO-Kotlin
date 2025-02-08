import kotlin.properties.Delegates

class BankAccount(amount: Int) {
    private var _balance: Int by Delegates.observable(0) { _, oldValue, newValue ->
        logTransaction(oldValue, newValue)
    }

    init {
        require(amount >= 0) { "Amount for init must be non-negative" }
        _balance = amount
    }

    val balance
        get() = _balance

    fun deposit(amount: Int) {
        require(amount > 0) { "Amount for deposit must be positive" }
        _balance += amount
    }

    fun withdraw(amount: Int) {
        require(amount > 0) { "Amount for withdraw must be positive" }
        require(_balance - amount >= 0) { "Balance after withdraw can't be negative" }
        _balance -= amount
    }

}

fun logTransaction(from: Int, to: Int) {
    println("$from -> $to")
}
