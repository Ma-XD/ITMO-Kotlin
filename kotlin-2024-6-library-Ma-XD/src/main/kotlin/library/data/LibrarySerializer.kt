package library.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import library.api.BookCatalog
import library.api.LibraryState
import nl.adaptivity.xmlutil.serialization.XML

object LibrarySerializer {
    private val xml = XML {
        indent = 4
    }
    private val json = Json { prettyPrint = true }

    fun decodeCatalog(string: String): BookCatalog = xml.decodeFromString(BookCatalog.serializer(), string)

    fun encodeCatalog(st: BookCatalog): String = xml.encodeToString(st)
        .replace("\n", System.lineSeparator())

    fun decodeState(string: String): LibraryState = json.decodeFromString(string)

    fun encodeState(state: LibraryState): String = json.encodeToString(state)
        .replace("\n", System.lineSeparator())
}
