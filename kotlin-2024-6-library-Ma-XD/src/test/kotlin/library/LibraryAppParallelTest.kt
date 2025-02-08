package library

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import library.api.Book
import library.api.BookCatalog
import library.api.BookDescription
import library.data.FileLibraryStorage
import library.data.LibrarySerializer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LibraryAppParallelTest {
    private lateinit var storagePath: Path
    private lateinit var library: LibraryApplication
    private lateinit var scope: CoroutineScope

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @BeforeEach
    fun before() {
        storagePath = Files.createTempDirectory(Path.of("."), "storage")
        storagePath.resolve("books.xml").writeText(LibrarySerializer.encodeCatalog(DEFAULT_BOOK_CATALOG))
        library = LibraryApplication(FileLibraryStorage(storagePath), AbstractStorageTest.Companion.NopEmailSender)
        scope = CoroutineScope(newSingleThreadContext("LibraryApp"))
        scope.launch {
            library.run()
        }
    }

    @AfterEach
    fun cleanup() {
        Files.walk(storagePath).toList().reversed().forEach { Files.deleteIfExists(it) }
        scope.cancel()
    }

    @Test
    fun testCreateUserParallel() {
        val USERS_COUNT = 1024
        val cnt = AtomicIntegerArray(USERS_COUNT)
        testRunParallel(16) { j ->
            for (i in 0..<USERS_COUNT) {
                try {
                    library.createUser("$i@example.com", "$i-$j")
                    cnt.incrementAndGet(i)
                } catch (ignore: IllegalArgumentException) {
                }
            }
        }

        for (i in 0..<USERS_COUNT) {
            Assertions.assertEquals(1, cnt.get(i), "User with email $i@example.com should be created once")
        }
    }

    @Test
    fun testBorrowBookParallel() {
        val USERS_COUNT = 8
        val REPEATS = 512
        val cnt = AtomicIntegerArray(USERS_COUNT)
        testRunParallel(USERS_COUNT) { j ->
            val user = library.createUser("$j@example.com", "$j")
            for (i in 0..<REPEATS) {
                var borrowBookId: Int? = null
                for (bookId in 1..USERS_COUNT) {
                    try {
                        library.borrowBook("$bookId", user.id)
                        borrowBookId = bookId
                        Assertions.assertEquals(
                            1,
                            cnt.incrementAndGet(bookId - 1),
                            "The book can only be taken by one user",
                        )
                        break
                    } catch (ignore: IllegalArgumentException) {
                    }
                }
                delay(5.milliseconds)
                Assertions.assertNotNull(borrowBookId, "User should can take book")
                cnt.decrementAndGet(borrowBookId!! - 1)
                library.returnBook("$borrowBookId", user.id)
            }
        }

        for (i in 0..<USERS_COUNT) {
            Assertions.assertEquals(0, cnt.get(i), "All books should be returned after test")
        }
    }

    companion object {
        val DEFAULT_BOOK_CATALOG = BookCatalog(
            (1..16).map { i ->
                Book("$i", "Book $i", "Author $i", 2000, BookDescription(""))
            },
        )

        @OptIn(DelicateCoroutinesApi::class)
        fun testRunParallel(nThreads: Int, block: suspend CoroutineScope.(i: Int) -> Unit) {
            var error: Throwable? = null
            runBlocking {
                newFixedThreadPoolContext(nThreads, "ctx").use { ctx ->
                    val handler = CoroutineExceptionHandler { _, throwable -> error = throwable }
                    val scope = CoroutineScope(ctx + handler)
                    (0..<nThreads).map { i ->
                        scope.launch { block(i) }
                    }.joinAll()
                }
            }
            if (error != null) {
                throw error!!
            }
        }
    }
}
