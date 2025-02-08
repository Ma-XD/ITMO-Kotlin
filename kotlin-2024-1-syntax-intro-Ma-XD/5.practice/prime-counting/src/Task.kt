import kotlin.math.abs
import kotlin.math.sqrt

fun isPrime(n: Int): Boolean {
    val sqrtN = sqrt(abs(n).toDouble()).toInt()
    for (i in 2..sqrtN) {
        if (n % i == 0) return false
    }
    return abs(n) > 1
}

fun piFunction(x: Double): Int {
    if (x < 2.0) return 0

    val n = x.toInt() + 1
    val isPrime = BooleanArray(n) { true }
    isPrime[0] = false
    isPrime[1] = false
    for (i in 2..sqrt(x).toInt()) {
        if (isPrime[i]) {
            for (j in i * i until n step i) {
                isPrime[j] = false
            }
        }
    }
    return isPrime.count { it }
}
