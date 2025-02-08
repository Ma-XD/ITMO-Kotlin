import java.io.InputStream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Config(fileName: String) {
    private val data: Map<String, String> = extractContent(fileName)

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, String> {
        val key = property.name
        val value = requireNotNull(data[key]) { "key '$key' is not provided" }
        return ReadOnlyProperty { _, _ -> value }
    }

    companion object {
        private fun extractContent(fileName: String): Map<String, String> {
            val inputStream = requireNotNull(getResource(fileName)) { "undefined resource by file '$fileName'" }
            return inputStream.bufferedReader().use { reader ->
                buildMap {
                    reader.forEachLine { line ->
                        check(line.count { it == '=' } == 1) {
                            "incorrect file format: expected only one '=' per line"
                        }
                        val (k, v) = line.split('=').map { it.trim() }
                        put(k, v)
                    }
                }
            }
        }
    }
}

@Suppress(
    "RedundantNullableReturnType",
    "UNUSED_PARAMETER",
)
fun getResource(fileName: String): InputStream? {
    // do not touch this function
    val content =
        """
        |valueKey = 10
        |otherValueKey = stringValue 
        """.trimMargin()

    return content.byteInputStream()
}
