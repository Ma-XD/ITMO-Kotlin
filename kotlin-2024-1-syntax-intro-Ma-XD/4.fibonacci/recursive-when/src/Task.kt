fun fibonacciWhen(n: Int): Int = when {
    n < 2 -> n
    else -> fibonacciWhen(n - 1) + fibonacciWhen(n - 2)
}
