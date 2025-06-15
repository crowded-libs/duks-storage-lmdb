package duks.storage.lmdb

import kotlinx.io.files.*
import lmdb.Env
import lmdb.EnvOption
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * iOS-specific implementation of test utilities.
 */
@OptIn(ExperimentalUuidApi::class)
actual fun createTestEnv(): TestEnvWrapper {
    val fs = SystemFileSystem
    // Use UUID to ensure unique paths
    val uuid = Uuid.random()
    val testPath = Path(SystemTemporaryDirectory.toString(), "lmdb-test-$uuid")
    
    if (!fs.exists(testPath)) {
        fs.createDirectories(testPath, true)
    }

    val env = Env()
    env.mapSize = 10UL * 1024UL * 1024UL // 10MB
    env.maxDatabases = 5u
    env.open(testPath.toString())

    return TestEnvWrapper(env, testPath.toString())
}