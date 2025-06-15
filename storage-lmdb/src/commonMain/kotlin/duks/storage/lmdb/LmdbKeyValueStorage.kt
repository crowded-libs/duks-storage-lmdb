package duks.storage.lmdb

import duks.logging.*
import lmdb.*

/**
 * General-purpose LMDB key-value storage implementation with built-in serialization.
 * This class provides type-safe key-value operations backed by LMDB.
 * 
 * @param T The type of values being stored
 * @param env The LMDB environment to use (must be already opened)
 * @param serializer Serializer for converting values to/from bytes
 * @param databaseName The name of the database within the LMDB environment
 */
class LmdbKeyValueStorage<T>(
    private val env: Env,
    private val serializer: Serializer<T>,
    private val databaseName: String
) {
    companion object {
        private val logger = Logger.default()
    }
    
    private val dbi: Dbi
    
    init {
        logger.debug(databaseName) {
            "Opening LMDB database {databaseName}"
        }
        val tx = env.beginTxn()
        try {
            dbi = tx.dbiOpen(databaseName, DbiOption.Create)
            tx.commit()
        } finally {
            tx.close()
        }
    }
    
    /**
     * Store a value with the given key.
     */
    suspend fun put(key: String, value: T) {
        val keyBytes = key.encodeToByteArray()
        val valueBytes = serializer.serialize(value)
        val size = valueBytes.size
        logger.debug(key, size) {
            "Storing value for key {key}, size: {size} bytes"
        }
        try {
            env.beginTxn {
                put(dbi, keyBytes, valueBytes)
                commit()
            }
        } catch (e: Exception) {
            logger.error(e, key) {
                "Failed to store value for key {key}"
            }
            throw e
        }
    }
    
    /**
     * Retrieve a value by key.
     * Returns null if the key doesn't exist.
     */
    suspend fun get(key: String): T? {
        val keyBytes = key.encodeToByteArray()
        var result: T? = null
        try {
            env.beginTxn(TxnOption.ReadOnly) {
                val getResult = get(dbi, keyBytes)
                if (getResult.resultCode == 0) {
                    val bytes = getResult.data.toByteArray()
                    if (bytes != null) {
                        result = serializer.deserialize(bytes)
                        logger.debug(key, true) {
                            "Retrieved value for key {key}, found: {found}"
                        }
                    }
                } else {
                    logger.debug(key, false) {
                        "Retrieved value for key {key}, found: {found}"
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e, key) {
                "Failed to retrieve value for key {key}"
            }
            throw e
        }
        return result
    }
    
    /**
     * Delete a value by key.
     */
    suspend fun delete(key: String) {
        val keyBytes = key.encodeToByteArray()
        logger.debug(key) {
            "Deleting value for key {key}"
        }
        try {
            env.beginTxn {
                delete(dbi, keyBytes)
                commit()
            }
        } catch (e: Exception) {
            logger.error(e, key) {
                "Failed to delete value for key {key}"
            }
            throw e
        }
    }
    
    /**
     * Check if a key exists in the database.
     */
    suspend fun exists(key: String): Boolean {
        val keyBytes = key.encodeToByteArray()
        var exists = false
        try {
            env.beginTxn(TxnOption.ReadOnly) {
                val getResult = get(dbi, keyBytes)
                exists = getResult.resultCode == 0
            }
        } catch (e: Exception) {
            logger.error(e, key) {
                "Failed to check existence of key {key}"
            }
            throw e
        }
        return exists
    }
    
    /**
     * Get all keys in the database.
     */
    suspend fun getAllKeys(): Set<String> {
        val keys = mutableSetOf<String>()
        try {
            env.beginTxn(TxnOption.ReadOnly) {
                val cursor = openCursor(dbi)
                cursor.use {
                    var result = cursor.first()
                    while (result.resultCode == 0) {
                        val key = result.key.toByteArray()
                        if (key != null) {
                            keys.add(key.decodeToString())
                        }
                        result = cursor.next()
                    }
                    val count = keys.size
                    logger.debug(count) {
                        "Retrieved {count} keys from database"
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to retrieve all keys"
            }
            throw e
        }
        return keys
    }
}