package duks.storage.lmdb

import kotlinx.io.files.*

/**
 * Creates a test storage with a random directory.
 * Platform-specific implementations handle directory creation.
 */
expect fun createTestStorage(): TestStorageWrapper

/**
 * Wrapper for test storage that includes the path and storage instance.
 */
data class TestStorageWrapper(
    val storage: LmdbDuksStorage,
    val path: String
) {
    fun close() {
        storage.close()
        // Clean up the directory
        try {
            val fs = SystemFileSystem
            fs.delete(Path(path), true)
        } catch (_: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}