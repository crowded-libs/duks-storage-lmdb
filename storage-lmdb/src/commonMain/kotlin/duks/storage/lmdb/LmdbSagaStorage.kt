package duks.storage.lmdb

import duks.SagaInstance
import duks.storage.PersistedSagaInstance
import duks.storage.SagaStorage
import lmdb.*

/**
 * LMDB-based implementation of SagaStorage for persisting saga instances.
 * Each saga instance is stored with its ID as the key.
 * 
 * @param env The LMDB environment to use
 * @param sagaSerializer Serializer for converting PersistedSagaInstance to/from bytes
 * @param databaseName The name of the database within the LMDB environment (default: "sagas")
 */
class LmdbSagaStorage(
    private val env: Env,
    private val sagaSerializer: Serializer<PersistedSagaInstance>,
    private val databaseName: String = "sagas"
) : SagaStorage {

    private val dbi: Dbi

    init {
        val tx = env.beginTxn()
        try {
            dbi = tx.dbiOpen(databaseName, DbiOption.Create)
            tx.commit()
        } finally {
            tx.close()
        }
    }
    
    override suspend fun save(sagaId: String, instance: SagaInstance<*>) {
        val persisted = PersistedSagaInstance(
            id = instance.id,
            sagaName = instance.sagaName,
            state = serializeState(instance.state as Any),
            startedAt = instance.startedAt,
            lastUpdatedAt = instance.lastUpdatedAt
        )
        
        val bytes = sagaSerializer.serialize(persisted)
        env.beginTxn {
            put(dbi, sagaId.encodeToByteArray(), bytes)
            commit()
        }
    }
    
    override suspend fun load(sagaId: String): SagaInstance<*>? {
        var result: SagaInstance<*>? = null
        env.beginTxn(TxnOption.ReadOnly) {
            val getResult = get(dbi, sagaId.encodeToByteArray())
            if (getResult.resultCode == 0) {
                val bytes = getResult.data.toByteArray()
                if (bytes != null) {
                    val persisted = sagaSerializer.deserialize(bytes)
                    result = deserializeSagaInstance(persisted)
                }
            }
        }
        return result
    }
    
    override suspend fun remove(sagaId: String) {
        env.beginTxn {
            delete(dbi, sagaId.encodeToByteArray())
            commit()
        }
    }
    
    override suspend fun getAllSagaIds(): Set<String> {
        val sagaIds = mutableSetOf<String>()
        env.beginTxn(TxnOption.ReadOnly) {
            val cursor = openCursor(dbi)
            cursor.use {
                var result = cursor.first()
                while (result.resultCode == 0) {
                    val key = result.key.toByteArray()
                    if (key != null) {
                        sagaIds.add(key.decodeToString())
                    }
                    result = cursor.next()
                }
            }
        }
        return sagaIds
    }
    
    
    /**
     * Serialize saga state to string representation.
     * This is a simplified implementation - in practice, you would need
     * a more sophisticated serialization strategy that can handle different state types.
     */
    private fun serializeState(state: Any): String {
        // For now, just use toString(). In a real implementation,
        // you would need a proper serialization strategy.
        return state.toString()
    }
    
    /**
     * Deserialize saga instance from persisted form.
     * This is a simplified implementation - in practice, you would need
     * a more sophisticated deserialization strategy that can reconstruct the correct state type.
     */
    private fun deserializeSagaInstance(persisted: PersistedSagaInstance): SagaInstance<*> {
        // For now, just use the state string as-is. In a real implementation,
        // you would need to properly deserialize based on the saga name.
        return SagaInstance(
            id = persisted.id,
            sagaName = persisted.sagaName,
            state = persisted.state, // This would need proper deserialization
            startedAt = persisted.startedAt,
            lastUpdatedAt = persisted.lastUpdatedAt
        )
    }
}

/**
 * Creates an LmdbSagaStorage instance with its own environment.
 * This constructor is provided for backward compatibility.
 * Consider using LmdbDuksStorage.createSagaStorage() for shared environment usage.
 */
fun LmdbSagaStorage(
    config: LmdbStorageConfig,
    sagaSerializer: Serializer<PersistedSagaInstance>,
    databaseName: String = "sagas"
): LmdbSagaStorage {
    val env = Env().apply {
        mapSize = config.mapSize.toULong()
        maxDatabases = config.maxDbs.toUInt()
        maxReaders = config.maxReaders.toUInt()
        open(
            config.path,
            *buildList {
                if (config.readOnly) add(EnvOption.ReadOnly)
                add(EnvOption.NoTLS)
            }.toTypedArray()
        )
    }
    
    return LmdbSagaStorage(
        env = env,
        sagaSerializer = sagaSerializer,
        databaseName = databaseName
    )
}

/**
 * Factory function to create an LmdbSagaStorage instance with proper saga state serialization.
 * This variant accepts a SagaStateSerializer from the duks library for proper state handling.
 * This constructor is provided for backward compatibility.
 * Consider using LmdbDuksStorage.createSagaStorage() for shared environment usage.
 */
fun createLmdbSagaStorage(
    config: LmdbStorageConfig,
    persistedSagaSerializer: Serializer<PersistedSagaInstance>,
    sagaStateSerializer: duks.storage.SagaStateSerializer
): SagaStorage {
    val env = Env().apply {
        mapSize = config.mapSize.toULong()
        maxDatabases = config.maxDbs.toUInt()
        maxReaders = config.maxReaders.toUInt()
        open(
            config.path,
            *buildList {
                if (config.readOnly) add(EnvOption.ReadOnly)
                add(EnvOption.NoTLS)
            }.toTypedArray()
        )
    }
    
    return LmdbSagaStorageWithStateSerializer(
        env = env,
        persistedSagaSerializer = persistedSagaSerializer,
        sagaStateSerializer = sagaStateSerializer
    )
}

/**
 * Internal implementation that uses the duks SagaStateSerializer for proper state handling.
 */
internal class LmdbSagaStorageWithStateSerializer(
    private val env: Env,
    private val persistedSagaSerializer: Serializer<PersistedSagaInstance>,
    private val sagaStateSerializer: duks.storage.SagaStateSerializer,
    private val databaseName: String = "sagas"
) : SagaStorage {
    
    private var dbi: Dbi? = null
    
    override suspend fun save(sagaId: String, instance: SagaInstance<*>) {
        val persisted = PersistedSagaInstance(
            id = instance.id,
            sagaName = instance.sagaName,
            state = sagaStateSerializer.serialize(instance.state as Any),
            startedAt = instance.startedAt,
            lastUpdatedAt = instance.lastUpdatedAt
        )
        
        val bytes = persistedSagaSerializer.serialize(persisted)
        env.beginTxn {
            val db = dbi ?: dbiOpen(databaseName, DbiOption.Create).also { dbi = it }
            put(db, sagaId.encodeToByteArray(), bytes)
            commit()
        }
    }
    
    override suspend fun load(sagaId: String): SagaInstance<*>? {
        // If we haven't opened the database yet, create it
        if (dbi == null) {
            env.beginTxn {
                dbi = dbiOpen(databaseName, DbiOption.Create)
                commit()
            }
        }
        
        var result: SagaInstance<*>? = null
        env.beginTxn(TxnOption.ReadOnly) {
            val db = dbi!!
            val getResult = get(db, sagaId.encodeToByteArray())
            if (getResult.resultCode == 0) {
                val bytes = getResult.data.toByteArray()
                if (bytes != null) {
                    val persisted = persistedSagaSerializer.deserialize(bytes)
                    val state = sagaStateSerializer.deserialize(persisted.state, persisted.sagaName)
                    result = SagaInstance(
                        id = persisted.id,
                        sagaName = persisted.sagaName,
                        state = state,
                        startedAt = persisted.startedAt,
                        lastUpdatedAt = persisted.lastUpdatedAt
                    )
                }
            }
        }
        return result
    }
    
    override suspend fun remove(sagaId: String) {
        if (dbi == null) return // Nothing to remove
        
        env.beginTxn {
            val db = dbi!!
            delete(db, sagaId.encodeToByteArray())
            commit()
        }
    }
    
    override suspend fun getAllSagaIds(): Set<String> {
        // If we haven't opened the database yet, create it
        if (dbi == null) {
            env.beginTxn {
                dbi = dbiOpen(databaseName, DbiOption.Create)
                commit()
            }
        }
        
        val sagaIds = mutableSetOf<String>()
        env.beginTxn(TxnOption.ReadOnly) {
            val db = dbi!!
            val cursor = openCursor(db)
            cursor.use {
                var result = cursor.first()
                while (result.resultCode == 0) {
                    val key = result.key.toByteArray()
                    if (key != null) {
                        sagaIds.add(key.decodeToString())
                    }
                    result = cursor.next()
                }
            }
        }
        return sagaIds
    }
    
}