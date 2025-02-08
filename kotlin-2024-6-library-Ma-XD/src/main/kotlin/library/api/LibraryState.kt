package library.api

import kotlinx.serialization.Serializable

@Serializable
data class LibraryState(
    var users: List<User>,
    var borrowedBooks: List<BorrowedBook>,
)
