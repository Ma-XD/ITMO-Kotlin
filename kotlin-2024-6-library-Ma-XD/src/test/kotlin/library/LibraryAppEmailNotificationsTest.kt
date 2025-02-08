package library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import library.api.*
import library.data.FileLibraryStorage
import library.data.LibrarySerializer
import library.notifications.EmailSender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LibraryAppEmailNotificationsTest {
    private lateinit var storagePath: Path
    private lateinit var library: LibraryApplication
    private lateinit var sender: TestEmailSender
    private lateinit var scope: CoroutineScope

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @BeforeEach
    fun before() {
        storagePath = Files.createTempDirectory(Path.of("."), "storage")
        storagePath.resolve("books.xml").writeText(LibrarySerializer.encodeCatalog(DEFAULT_BOOK_CATALOG))
        storagePath.resolve("state.json").writeText(LibrarySerializer.encodeState(LibraryState(USERS, BORROWED_BOOKS)))
        sender = TestEmailSender()
        library = LibraryApplication(FileLibraryStorage(storagePath), sender)
        scope = CoroutineScope(newSingleThreadContext("LibraryApp"))
        scope.launch {
            library.run()
        }
    }

    @AfterEach
    fun cleanup() {
        scope.cancel()
        Files.walk(storagePath).toList().reversed().forEach { Files.deleteIfExists(it) }
    }

    @Test
    fun test1() = runBlocking {
        // In this test, books 1 and 2 are not returned by user 1, and book 3 is not submitted by user 3 (see BORROWED_BOOKS)
        library.sendOverdueBooksNotification()

        delay(100.milliseconds)
        val emails = sender.getEmails()
        Assertions.assertEquals(
            setOf("1@example.com", "3@example.com"),
            emails.map { it.first }.toSet(),
        )
    }

    @Test
    fun test2() = runBlocking {
        // In this test, books 1 and 2 are not returned by user 1, and book 3 is not submitted by user 3 (see BORROWED_BOOKS)
        // User 2 borrow book 4 just now and shouldn't return it yet
        library.borrowBook("book-4", "user-2")

        library.sendOverdueBooksNotification()

        delay(100.milliseconds)
        val emails = sender.getEmails()
        Assertions.assertEquals(
            setOf("1@example.com", "3@example.com"),
            emails.map { it.first }.toSet(),
        )
    }

    @Test
    fun test3() = runBlocking {
        // In this test, books 1 and 2 are not returned by user 1, and book 3 is not submitted by user 3 (see BORROWED_BOOKS)
        // But user 3 returned book 3 first
        library.returnBook("book-3", "user-3")

        library.sendOverdueBooksNotification()

        delay(100.milliseconds)
        val emails = sender.getEmails()
        Assertions.assertEquals(setOf("1@example.com"), emails.map { it.first }.toSet())
    }

    @Test
    fun test4() = runBlocking {
        // In this test, books 1 and 2 are not returned by user 1, and book 3 is not submitted by user 3 (see BORROWED_BOOKS)
        // But users returned all books

        library.returnBook("book-1", "user-1")
        library.returnBook("book-2", "user-1")
        library.returnBook("book-3", "user-3")

        library.sendOverdueBooksNotification()

        delay(100.milliseconds)
        val emails = sender.getEmails()
        Assertions.assertTrue(emails.isEmpty())
    }

    class TestEmailSender : EmailSender {
        private val mutex = Mutex()
        private val emails = mutableListOf<Pair<String, String>>()

        override suspend fun send(to: String, subject: String, text: String) {
            mutex.withLock {
                emails.add(to to text)
            }
        }

        suspend fun getEmails(): List<Pair<String, String>> {
            return mutex.withLock {
                val current = emails.toList()
                emails.clear()
                current
            }
        }
    }

    companion object {
        val DEFAULT_BOOK_CATALOG = BookCatalog(
            (1..4).map { i ->
                Book("book-$i", "Book $i", "Author $i", 2000, BookDescription(""))
            },
        )
        val USERS = (1..3).map { User("user-$it", "$it@example.com", "Author $it") }
        val BORROWED_BOOKS = listOf(
            BorrowedBook("book-1", "user-1", Clock.System.now()),
            BorrowedBook("book-2", "user-1", Clock.System.now()),
            BorrowedBook("book-3", "user-3", Clock.System.now()),
        )
    }
}
