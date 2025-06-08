package duks.storage.lmdb

/**
 * Configuration for LMDB storage instances.
 * 
 * @property path The file system path where the LMDB environment will be created
 * @property maxDbs The maximum number of named databases in the environment (default: 10)
 * @property mapSize The maximum size of the LMDB memory map in bytes (default: 10MB)
 * @property maxReaders The maximum number of reader threads (default: 126)
 * @property readOnly Whether to open the database in read-only mode (default: false)
 */
data class LmdbStorageConfig(
    val path: String,
    val maxDbs: Int = 10,
    val mapSize: Long = 10 * 1024 * 1024, // 10MB default
    val maxReaders: Int = 126,
    val readOnly: Boolean = false
) {
    init {
        require(path.isNotBlank()) { "Path must not be blank" }
        require(maxDbs > 0) { "maxDbs must be greater than 0" }
        require(mapSize > 0) { "mapSize must be greater than 0" }
        require(maxReaders > 0) { "maxReaders must be greater than 0" }
    }
}