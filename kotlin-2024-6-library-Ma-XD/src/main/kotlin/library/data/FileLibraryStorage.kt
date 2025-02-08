package library.data

import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import library.api.*

class FileLibraryStorage(storagePath: Path) : LibraryStorage {
    private val booksPath = storagePath.resolve("books.xml")
    private val statePath = storagePath.resolve("state.json")
    private var users: List<User> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                writeState(LibraryState(field, borrowedBooks))
            }
        }
    private var borrowedBooks: List<BorrowedBook> = emptyList()
        set(value) {
            if (field != value) {
                field = value
                writeState(LibraryState(users, field))
            }
        }

    init {
        if (statePath.exists()) {
            val state = readState()
            users = state.users
            borrowedBooks = state.borrowedBooks
        }
    }

    override suspend fun allBooks(): List<Book> {
        return readBooks().books
    }

    override suspend fun allowedBooks(): List<Book> {
        val borrowedIds = borrowedBooks.map { it.bookId }.toSet()
        return allBooks().filter { !borrowedIds.contains(it.id) }
    }

    override suspend fun borrowedBooksInfo(): List<BorrowedBook> {
        return borrowedBooks
    }

    override suspend fun borrowBook(bookId: String, userId: String): Instant {
        val user = findUser(userId)

        val book = allowedBooks().find { it.id == bookId }
        requireNotNull(book) { "Book with id $bookId is not allow." }

        val borrowedBook = BorrowedBook(
            bookId = book.id,
            userId = user.id,
            returnDeadline = Clock.System.now() + 7.days,
        )

        borrowedBooks += borrowedBook

        return borrowedBook.returnDeadline
    }

    override suspend fun returnBook(bookId: String, userId: String) {
        findUser(userId)

        val borrowedBook = borrowedBooks.find { it.bookId == bookId }
        requireNotNull(borrowedBook) { "Book with id $bookId is not borrowed." }

        borrowedBooks -= borrowedBook
    }

    override suspend fun createUser(email: String, name: String): User {
        require(users.all { it.email != email }) { "User with email $email already exists." }

        val user = User(
            id = UUID.randomUUID().toString(),
            email = email,
            name = name,
        )

        users += user

        return user
    }

    override suspend fun findUser(userId: String): User {
        val user = users.find { it.id == userId }
        return requireNotNull(user) { "User with id $userId does not exist." }
    }

    private fun readBooks(): BookCatalog {
        return LibrarySerializer.decodeCatalog(booksPath.readText())
    }

    private fun readState(): LibraryState {
        return LibrarySerializer.decodeState(statePath.readText())
    }

    private fun writeState(state: LibraryState) {
        statePath.writeText(LibrarySerializer.encodeState(state))
    }
}
