package library.api

import kotlinx.serialization.Serializable

@Serializable
data class BookCatalog(val books: List<Book>)
