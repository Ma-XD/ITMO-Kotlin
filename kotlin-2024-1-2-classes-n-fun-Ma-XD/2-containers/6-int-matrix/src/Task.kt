class IntMatrix(val rows: Int, val columns: Int) {
    private val data: IntArray

    init {
        require(rows > 0 && columns > 0) { "rows and columns must be positive" }
        data = IntArray(rows * columns)
    }

    operator fun get(row: Int, column: Int): Int {
        return data[index(row, column)]
    }

    operator fun set(row: Int, column: Int, value: Int) {
        data[index(row, column)] = value
    }

    private fun index(row: Int, column: Int): Int {
        require(row in 0..<rows) { "row must be in [0, $rows)" }
        require(column in 0..<columns) { "column must be in [0, $columns)" }
        return row * columns + column
    }
}

fun main() {
    val matrix = IntMatrix(3, 4)
    println(matrix.rows)
    println(matrix.columns)
    println(matrix[0, 0])
    matrix[2, 3] = 42
    println(matrix[2, 3])
}
