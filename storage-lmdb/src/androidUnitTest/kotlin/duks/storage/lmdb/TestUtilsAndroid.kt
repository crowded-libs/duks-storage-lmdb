package duks.storage.lmdb

import lmdb.Env
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * Android unit test implementation of test utilities.
 * 
 * Android unit tests run on the host JVM.
 * kotlin-lmdb 0.3.1 includes host platform libraries for testing.
 */
@OptIn(ExperimentalUuidApi::class)
actual fun createTestEnv(): TestEnvWrapper {
    val uuid = Uuid.random().toString().take(8)
    val tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
    val testPath = "$tempDir/lmdb-test-$uuid"
    
    // Create directory
    val dir = java.io.File(testPath)
    if (!dir.mkdirs()) {
        throw RuntimeException("Failed to create test directory: $testPath")
    }
    
    val env = Env()
    env.mapSize = 10UL * 1024UL * 1024UL
    env.maxDatabases = 5u
    env.open(testPath)
    return TestEnvWrapper(env, testPath)
}