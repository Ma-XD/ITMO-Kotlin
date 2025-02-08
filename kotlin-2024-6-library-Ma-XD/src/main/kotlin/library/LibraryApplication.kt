package library

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import library.api.Book
import library.api.BorrowedBook
import library.api.User
import library.data.LibraryStorage
import library.notifications.EmailSender

private sealed class LibraryAction
private sealed class StorageAction<T> : LibraryAction() {
    val response: CompletableDeferred<T> = CompletableDeferred()
}

private class AllBooksAction : StorageAction<List<Book>>()
private class AllowedBooksAction : StorageAction<List<Book>>()
private class BorrowedBooksInfoAction : StorageAction<List<BorrowedBook>>()
private class BorrowBookAction(val bookId: String, val userId: String) : StorageAction<Instant>()
private class ReturnBookAction(val bookId: String, val userId: String) : StorageAction<Unit>()
private class CreateUserAction(val email: String, val name: String) : StorageAction<User>()
private class FindUserAction(val userId: String) : StorageAction<User>()
private data object SendEmailAction : LibraryAction()

class LibraryApplication(
    private val storage: LibraryStorage,
    private val emailSender: EmailSender,
) : LibraryStorage {
    private val updatesFlow = MutableSharedFlow<LibraryAction>(replay = 1)

    override suspend fun allBooks(): List<Book> = AllBooksAction().await()

    override suspend fun allowedBooks(): List<Book> = AllowedBooksAction().await()

    override suspend fun borrowedBooksInfo(): List<BorrowedBook> = BorrowedBooksInfoAction().await()

    override suspend fun borrowBook(bookId: String, userId: String): Instant = BorrowBookAction(bookId, userId).await()

    override suspend fun returnBook(bookId: String, userId: String): Unit = ReturnBookAction(bookId, userId).await()

    override suspend fun createUser(email: String, name: String): User = CreateUserAction(email, name).await()

    override suspend fun findUser(userId: String): User = FindUserAction(userId).await()

    suspend fun sendOverdueBooksNotification() {
        updatesFlow.emit(SendEmailAction)
    }

    suspend fun run() {
        updatesFlow.collect { action ->
            when (action) {
                is AllBooksAction -> action.complete { storage.allBooks() }
                is AllowedBooksAction -> action.complete { storage.allowedBooks() }
                is BorrowedBooksInfoAction -> action.complete { storage.borrowedBooksInfo() }
                is BorrowBookAction -> action.complete { storage.borrowBook(action.bookId, action.userId) }
                is ReturnBookAction -> action.complete { storage.returnBook(action.bookId, action.userId) }
                is CreateUserAction -> action.complete { storage.createUser(action.email, action.name) }
                is FindUserAction -> action.complete { storage.findUser(action.userId) }
                is SendEmailAction -> sendNotification()
            }
        }
    }

    private suspend fun <T> StorageAction<T>.await(): T {
        updatesFlow.emit(this)
        return this.response.await()
    }

    private suspend fun <T> StorageAction<T>.complete(function: suspend () -> T) {
        try {
            this.response.complete(function())
        } catch (e: Exception) {
            this.response.completeExceptionally(e)
        }
    }

    private suspend fun sendNotification() {
        val now = Clock.System.now()
        storage.borrowedBooksInfo()
            .filter { it.returnDeadline < now }
            .groupingBy { it.userId }
            .eachCount()
            .forEach { (userId, booksCount) ->
                val user = storage.findUser(userId)
                emailSender.send(
                    user.email,
                    "Where's the book, Lebowski?",
                    notificationText(user.name, booksCount),
                )
            }
    }

    companion object {
        private fun notificationText(name: String, count: Int) = """
            Dear, $name!
            You didn't return $count books with expired return date. Please return them as soon as possible.
            Best regards.
            """.trimIndent()
    }
}
