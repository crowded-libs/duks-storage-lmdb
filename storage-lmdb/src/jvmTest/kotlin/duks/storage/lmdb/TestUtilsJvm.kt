package duks.storage.lmdb

import kotlinx.io.files.*
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

/**
 * JVM-specific implementation of test utilities.
 */
@OptIn(ExperimentalUuidApi::class)
actual fun createTestStorage(): TestStorageWrapper {
    val fs = SystemFileSystem
    // Use UUID to ensure unique paths
    val uuid = Uuid.random()
    val testPath = Path(SystemTemporaryDirectory.toString(), "lmdb-test-$uuid")
    
    if (!fs.exists(testPath)) {
        fs.createDirectories(testPath, true)
    }
    
    val config = LmdbStorageConfig(
        path = testPath.toString(),
        mapSize = 10485760L, // 10MB
        maxDbs = 10
    )
    
    val storage = LmdbDuksStorage(config)
    
    return TestStorageWrapper(storage, testPath.toString())
}