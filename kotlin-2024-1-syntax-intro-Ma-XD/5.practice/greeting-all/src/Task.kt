fun greet(name: String) = "Hello, $name!"

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        args.forEach { println(greet(it)) }
    } else {
        println(greet(readlnOrNull() ?: "Anonymous"))
    }
}
