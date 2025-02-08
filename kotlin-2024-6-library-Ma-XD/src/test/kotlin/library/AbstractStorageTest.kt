package library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import library.api.*
import library.data.LibrarySerializer
import library.data.LibraryStorage
import library.notifications.EmailSender
import org.junit.jupiter.api.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
abstract class AbstractStorageTest {
    protected lateinit var storagePath: Path
    protected lateinit var storage: LibraryStorage

    @BeforeEach
    fun before() {
        storagePath = Files.createTempDirectory(Path.of("."), "storage")
    }

    @AfterEach
    fun cleanup() {
        Files.walk(storagePath).toList().reversed().forEach { Files.deleteIfExists(it) }
    }

    @Test
    @Order(1)
    fun testFindUser() = runTest {
        prepareStorage(emptyList(), listOf(USER1, USER2), emptyList())

        val user1 = storage.findUser(USER1.id)
        Assertions.assertEquals(USER1.name, user1.name, "find user should return correct user")
        Assertions.assertEquals(USER1.email, user1.email, "find user should return correct user")

        val user2 = storage.findUser(USER2.id)
        Assertions.assertEquals(USER2.name, user2.name, "find user should return correct user")
        Assertions.assertEquals(USER2.email, user2.email, "find user should return correct user")

        assertThrows(IllegalArgumentException::class.java) {
            storage.findUser(USER3.id)
        }
    }

    @Test
    @Order(2)
    fun testCreatingUser() = runTest {
        prepareStorage(emptyList(), emptyList(), emptyList())
        val user = storage.createUser("test@gmail.com", "Test Testov")
        Assertions.assertEquals(user.name, "Test Testov")
        Assertions.assertEquals(user.email, "test@gmail.com")

        val users = readState().users
        Assertions.assertEquals(1, users.size, "Should be one user after creation user")
        Assertions.assertEquals("Test Testov", users[0].name, "Created user should have correct name")
        Assertions.assertEquals("test@gmail.com", users[0].email, "Created user should have correct email")

        val user1 = storage.findUser(user.id)
        Assertions.assertEquals(user.name, user1.name, "find user should return correct user")
        Assertions.assertEquals(user.email, user1.email, "find user should return correct user")
    }

    @Test
    @Order(3)
    fun testCreatingUserCorrectId() = runTest {
        prepareStorage(emptyList(), listOf(USER1, USER3), emptyList())
        storage.createUser("test@gmail.com", "Second")

        val users = readState().users
        Assertions.assertEquals(3, users.size, "Should be 3 users after creation user")
        Assertions.assertEquals(3, users.map { it.id }.distinct().size, "All users should have unique ids")
    }

    @Test
    @Order(4)
    fun testCreateUserWithUsedEmail() = runTest {
        prepareStorage(emptyList(), listOf(USER1, USER2), emptyList())
        assertThrows(IllegalArgumentException::class.java) {
            storage.createUser(USER1.email, "test")
        }

        val users = readState().users
        Assertions.assertEquals(2, users.size, "Should be 2 users after invalid creation user")
        Assertions.assertEquals(listOf("1", "2"), users.map { it.name }.toList())
    }

    @Test
    @Order(11)
    fun testBooks() = runTest {
        prepareStorage(listOf(BOOK1, BOOK2), emptyList(), emptyList())

        val books = setOf(BOOK1.title, BOOK2.title)
        Assertions.assertEquals(
            books,
            storage.allBooks().map { it.title }.toSet(),
            "Method all books should return correct list of books",
        )
        Assertions.assertEquals(
            books,
            storage.allowedBooks().map { it.title }.toSet(),
            "Method allowed books should return correct list of books",
        )
    }

    @Test
    @Order(12)
    fun testBorrowBook() = runTest {
        prepareStorage(listOf(BOOK1, BOOK2), listOf(USER1), emptyList())

        Assertions.assertEquals(2, storage.allBooks().size)
        Assertions.assertEquals(2, storage.allowedBooks().size)

        storage.borrowBook(BOOK2.id, USER1.id)

        Assertions.assertEquals(2, storage.allBooks().size)
        storage.allowedBooks().let { books ->
            Assertions.assertEquals(1, books.size)
            Assertions.assertEquals(BOOK1.title, books[0].title)
        }
        Assertions.assertEquals(1, readState().borrowedBooks.size)

        storage.borrowBook(BOOK1.id, USER1.id)

        Assertions.assertEquals(2, storage.allBooks().size)
        Assertions.assertEquals(0, storage.allowedBooks().size)

        Assertions.assertEquals(2, readState().borrowedBooks.size)
    }

    @Test
    @Order(13)
    fun testBorrowedBookLoad() = runTest {
        prepareStorage(
            listOf(BOOK1, BOOK2),
            listOf(USER1, USER2),
            listOf(BorrowedBook(BOOK1.id, USER2.id, Clock.System.now())),
        )

        storage.borrowedBooksInfo().let { borrowedBooks ->
            Assertions.assertEquals(
                1,
                borrowedBooks.size,
                "Information about borrowed book should be read from state file",
            )
            Assertions.assertEquals(BOOK1.id, borrowedBooks[0].bookId)
            Assertions.assertEquals(USER2.id, borrowedBooks[0].userId)
        }

        storage.allowedBooks().let { allowedBooks ->
            Assertions.assertEquals(1, allowedBooks.size, "Only one book should be allowed when one book was borrowed")
            Assertions.assertEquals(BOOK2.id, allowedBooks[0].id)
        }
    }

    @Test
    @Order(14)
    fun testBorrowAlreadyTakenBook() = runTest {
        prepareStorage(
            listOf(BOOK1, BOOK2),
            listOf(USER1, USER2),
            listOf(BorrowedBook(BOOK1.id, USER2.id, Clock.System.now())),
        )

        assertThrows(IllegalArgumentException::class.java) {
            storage.borrowBook(BOOK1.id, USER1.id)
        }

        Assertions.assertEquals(1, readState().borrowedBooks.size, "Number of borrowed book shouldn't changed")
    }

    @Test
    @Order(15)
    fun testBorrowMissingBook() = runTest {
        prepareStorage(listOf(), listOf(USER1), listOf())

        assertThrows(IllegalArgumentException::class.java) {
            storage.borrowBook(BOOK1.id, USER1.id)
        }

        Assertions.assertEquals(0, readState().borrowedBooks.size, "Number of borrowed book shouldn't changed")
    }

    @Test
    @Order(21)
    fun testReturnBook() = runTest {
        prepareStorage(listOf(BOOK1), listOf(USER3), listOf(BorrowedBook(BOOK1.id, USER3.id, Clock.System.now())))

        Assertions.assertEquals(1, storage.borrowedBooksInfo().size)
        Assertions.assertEquals(0, storage.allowedBooks().size)

        storage.returnBook(BOOK1.id, USER3.id)
        Assertions.assertEquals(
            0,
            storage.borrowedBooksInfo().size,
            "Should be no borrowed books aster return taken books",
        )
        Assertions.assertEquals(1, storage.allowedBooks().size, "Book should be available after return")
        Assertions.assertEquals(
            0,
            readState().borrowedBooks.size,
            "Should be no borrowed books in state file aster return taken books",
        )
    }

    @Test
    @Order(22)
    fun testReturnBookAndBorrowAgain() = runTest {
        prepareStorage(
            books = listOf(BOOK1),
            users = listOf(USER3),
            borrowedBooks = listOf(BorrowedBook(BOOK1.id, USER3.id, Clock.System.now())),
        )

        storage.returnBook(BOOK1.id, USER3.id)

        storage.borrowBook(BOOK1.id, USER3.id)

        Assertions.assertEquals(1, storage.borrowedBooksInfo().size)
        Assertions.assertEquals(0, storage.allowedBooks().size)

        storage.returnBook(BOOK1.id, USER3.id)
        Assertions.assertEquals(
            0,
            storage.borrowedBooksInfo().size,
            "Should be no borrowed books aster return taken books",
        )
        Assertions.assertEquals(1, storage.allowedBooks().size, "Book should be available after return")
        Assertions.assertEquals(
            0,
            readState().borrowedBooks.size,
            "Should be no borrowed books in state file aster return taken books",
        )
    }

    @Test
    @Order(23)
    fun testCreateUserAndBorrowBook() = runTest {
        prepareStorage(
            books = listOf(BOOK1, BOOK2, BOOK3),
            users = listOf(USER1, USER2),
            borrowedBooks = listOf(BorrowedBook(BOOK1.id, USER1.id, Clock.System.now())),
        )

        storage.borrowBook(BOOK2.id, USER2.id)

        Assertions.assertEquals(2, storage.borrowedBooksInfo().size)

        val user3 = storage.createUser(USER3.email, USER3.name)
        storage.borrowBook(BOOK3.id, user3.id)
        Assertions.assertEquals(3, storage.borrowedBooksInfo().size)
        Assertions.assertEquals(0, storage.allowedBooks().size)
    }

    @Test
    @Order(24)
    fun testBorrowAndReturn() = runTest {
        prepareStorage(
            books = listOf(BOOK1, BOOK2, BOOK3),
            users = listOf(USER1, USER2, USER3),
        )

        storage.borrowBook(BOOK1.id, USER3.id)
        Assertions.assertTrue(BOOK1.id to USER3.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })

        storage.borrowBook(BOOK2.id, USER1.id)
        Assertions.assertTrue(BOOK1.id to USER3.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK2.id to USER1.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })

        storage.returnBook(BOOK1.id, USER3.id)
        Assertions.assertTrue(BOOK1.id to USER3.id !in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK2.id to USER1.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })

        storage.borrowBook(BOOK1.id, USER2.id)
        storage.borrowBook(BOOK3.id, USER3.id)
        Assertions.assertTrue(BOOK2.id to USER1.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK1.id to USER2.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK3.id to USER3.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })

        storage.returnBook(BOOK1.id, USER2.id)
        storage.returnBook(BOOK2.id, USER1.id)

        storage.borrowBook(BOOK2.id, USER2.id)
        storage.borrowBook(BOOK1.id, USER1.id)

        Assertions.assertTrue(BOOK1.id to USER1.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK2.id to USER2.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
        Assertions.assertTrue(BOOK3.id to USER3.id in storage.borrowedBooksInfo().map { it.bookId to it.userId })
    }

    protected abstract fun setupStorage()

    private fun prepareStorage(
        books: List<Book>,
        users: List<User>,
        borrowedBooks: List<BorrowedBook> = emptyList(),
    ) {
        writeBooks(BookCatalog(books))
        writeState(
            LibraryState(users, borrowedBooks),
        )
        setupStorage()
    }

    private fun readBooks(): BookCatalog {
        return LibrarySerializer.decodeCatalog(storagePath.resolve("books.xml").readText())
    }

    private fun readState(): LibraryState {
        return LibrarySerializer.decodeState(storagePath.resolve("state.json").readText())
    }

    private fun writeBooks(books: BookCatalog) {
        storagePath.resolve("books.xml").writeText(LibrarySerializer.encodeCatalog(books))
    }

    private fun writeState(state: LibraryState) {
        storagePath.resolve("state.json").writeText(LibrarySerializer.encodeState(state))
    }

    private fun <T : Throwable> assertThrows(clazz: Class<T>, body: suspend () -> Unit) {
        Assertions.assertThrows(clazz) {
            runBlocking { body() }
        }
    }

    companion object {
        val USER1 = User("1", "first@itmo.ru", "1")
        val USER2 = User("2", "second@itmo.ru", "2")
        val USER3 = User("3", "third@itmo.ru", "3")

        val BOOK1 = Book(
            "10001",
            "1984",
            "George Orwell",
            1949,
            BookDescription(""),
        )
        val BOOK2 = Book(
            "10002",
            "Harry Potter and the Philosopher's Stone",
            "J. K. Rowling",
            1997,
            BookDescription(""),
        )
        val BOOK3 = Book(
            "10003",
            "Brave New World",
            "Aldous Huxley",
            1932,
            BookDescription("You know what is 2"),
        )

        object NopEmailSender : EmailSender {
            override suspend fun send(to: String, subject: String, text: String) {
                println("fsada")
            }
        }
    }
}
