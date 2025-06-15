package duks.storage.lmdb

import kotlinx.io.files.*
import lmdb.Env

/**
 * Creates a test environment with a random directory.
 * Platform-specific implementations handle directory creation.
 */
expect fun createTestEnv(): TestEnvWrapper

/**
 * Wrapper for test environment that includes the path and env instance.
 */
data class TestEnvWrapper(
    val env: Env,
    val path: String
) {
    fun close() {
        env.close()
        // Clean up the directory
        try {
            val fs = SystemFileSystem
            fs.delete(Path(path), true)
        } catch (_: Exception) {
            // Ignore cleanup errors in tests
        }
    }
}