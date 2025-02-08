package library

import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import library.api.*
import library.data.LibrarySerializer
import org.junit.jupiter.api.Assertions

class LibrarySerializerTest {

    @Test
    fun testBooks() {
        val catalog = BookCatalog(books)
        val encoded = LibrarySerializer.encodeCatalog(catalog)
        val decoded = LibrarySerializer.decodeCatalog(encoded)
        Assertions.assertEquals(books, decoded.books, "Books should serialise and deserialize correctly")
    }

    @Test
    fun testBooksFormatting() {
        val expectedText = testBooksPath.readText()
        val actualText = LibrarySerializer.encodeCatalog(LibrarySerializer.decodeCatalog(expectedText))
        Assertions.assertEquals(expectedText, actualText, "Invalid serialisation formatting")
    }

    @Test
    fun testState() {
        val encoded = LibrarySerializer.encodeState(state)
        val decoded = LibrarySerializer.decodeState(encoded)
        Assertions.assertEquals(state.users, decoded.users, "Users in state should serialise and deserialize correctly")
        Assertions.assertEquals(
            state.borrowedBooks,
            decoded.borrowedBooks,
            "Borrowed books should serialise and deserialize correctly",
        )
    }

    companion object {
        val testBooksPath = LibrarySerializerTest::class.java.classLoader.getResource("test-books.xml")
            ?: throw IllegalArgumentException("No test books file found")
        val testStatePath = LibrarySerializerTest::class.java.classLoader.getResource("test-state.json")
            ?: throw IllegalArgumentException("No test state file found")

        val books = listOf(
            Book(
                "1",
                "1984",
                "George Orwell",
                1949,
                BookDescription(
                    "Novel about a man’s struggle against a totalitarian regime that controls truth " +
                        "and suppresses individual freedom.",
                    genres = listOf(
                        "dystopian",
                        "political fiction",
                        "science fiction",
                    ),
                ),
            ),
            Book(
                "2",
                "Harry Potter and the Philosopher's Stone",
                "J. K. Rowling",
                1997,
                BookDescription(
                    "a young boy, Harry Potter, who discovers he's a wizard and embarks on an adventure at " +
                        "Hogwarts School of Witchcraft and Wizardry, " +
                        "where he uncovers secrets about his past and faces dark forces",
                    genres = listOf("fantasy"),
                ),
            ),
        )

        val state = LibraryState(
            users = listOf(User("1", "kbats", "kbats")),
            borrowedBooks = listOf(
                BorrowedBook("1", "1", Clock.System.now() + 1.days),
                BorrowedBook("2", "1", Clock.System.now()),
            ),
        )
    }
}
