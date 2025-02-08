package library

import library.data.FileLibraryStorage

class FileLibraryStorageTest : AbstractStorageTest() {
    override fun setupStorage() {
        storage = FileLibraryStorage(storagePath)
    }
}
