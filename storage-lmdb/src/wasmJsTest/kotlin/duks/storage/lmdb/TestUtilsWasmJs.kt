package duks.storage.lmdb

import lmdb.Env
import lmdb.WasmUtils
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * WasmJS-specific implementation of test utilities.
 * This handles the special case where LMDB needs to create directories
 * in the WASM filesystem.
 */

@OptIn(ExperimentalUuidApi::class)
actual fun createTestEnv(): TestEnvWrapper {
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
    
    val env = Env()
    env.mapSize = 10UL * 1024UL * 1024UL
    env.maxDatabases = 5u
    env.open(testPath)

    return TestEnvWrapper(env, testPath)
}