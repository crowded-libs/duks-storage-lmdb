package duks.storage.lmdb

import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * Android unit test implementation of test utilities.
 * 
 * Android unit tests run on the host JVM.
 * kotlin-lmdb 0.3.1 includes host platform libraries for testing.
 */
@OptIn(ExperimentalUuidApi::class)
actual fun createTestStorage(): TestStorageWrapper {
    val uuid = Uuid.random().toString().take(8)
    val tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
    val testPath = "$tempDir/lmdb-test-$uuid"
    
    // Create directory
    val dir = java.io.File(testPath)
    if (!dir.mkdirs()) {
        throw RuntimeException("Failed to create test directory: $testPath")
    }
    
    val config = LmdbStorageConfig(
        path = testPath,
        mapSize = 10485760L, // 10MB
        maxDbs = 10
    )
    val storage = LmdbDuksStorage(config)
    return TestStorageWrapper(storage, testPath)
}