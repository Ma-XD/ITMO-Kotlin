package library

import kotlinx.coroutines.*
import library.data.FileLibraryStorage
import org.junit.jupiter.api.AfterEach

class LibraryAppStorageTest : AbstractStorageTest() {
    private lateinit var library: LibraryApplication
    private lateinit var scope: CoroutineScope

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override fun setupStorage() {
        library = LibraryApplication(FileLibraryStorage(storagePath), Companion.NopEmailSender)
        storage = library
        scope = CoroutineScope(newSingleThreadContext("LibraryApp"))
        scope.launch {
            library.run()
        }
    }

    @AfterEach
    fun closeScope() {
        scope.cancel()
    }
}
