package library.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class BorrowedBook(
    val bookId: String,
    val userId: String,
    val returnDeadline: Instant,
)
