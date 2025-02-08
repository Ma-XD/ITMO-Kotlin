package library.data

import kotlinx.datetime.Instant
import library.api.*

interface LibraryStorage {
    suspend fun allBooks(): List<Book>
    suspend fun allowedBooks(): List<Book>
    suspend fun borrowedBooksInfo(): List<BorrowedBook>
    suspend fun borrowBook(bookId: String, userId: String): Instant
    suspend fun returnBook(bookId: String, userId: String)
    suspend fun createUser(email: String, name: String): User
    suspend fun findUser(userId: String): User
}
