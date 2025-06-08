package duks.storage.lmdb

import lmdb.WasmUtils
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * WasmJS-specific implementation of test utilities.
 * This handles the special case where LMDB needs to create directories
 * in the WASM filesystem.
 */

@OptIn(ExperimentalUuidApi::class)
actual fun createTestStorage(): TestStorageWrapper {
    // For WasmJS Node environment, NODEFS is mounted at /tmp -> .
    // We need to use /tmp path which maps to the current directory

    val uuid = Uuid.random().toString().take(8)
    val testPath = "/tmp/lmdb-test-$uuid"
    try {
        println("Creating directory: $testPath")
        WasmUtils.createDirectory(testPath)
    } catch(e: Exception) {
        println(e)
    }
    
    val config = LmdbStorageConfig(
        path = testPath,
        mapSize = 10485760L, // 10MB
        maxDbs = 10
    )
    
    val storage = LmdbDuksStorage(config)
    
    return TestStorageWrapper(storage, testPath)
}