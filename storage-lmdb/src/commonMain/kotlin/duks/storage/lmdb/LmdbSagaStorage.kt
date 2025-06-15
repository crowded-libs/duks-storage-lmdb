package duks.storage.lmdb

import duks.SagaInstance
import duks.storage.PersistedSagaInstance
import duks.storage.SagaStorage
import duks.logging.*
import lmdb.Env

/**
 * LMDB-based implementation of SagaStorage for persisting saga instances.
 * Each saga instance is stored with its ID as the key.
 * 
 * @param env The LMDB environment to use (must be already opened)
 * @param sagaSerializer Serializer for converting PersistedSagaInstance to/from bytes
 * @param databaseName The name of the database within the LMDB environment (default: "sagas")
 */
class LmdbSagaStorage(
    env: Env,
    sagaSerializer: Serializer<PersistedSagaInstance>,
    databaseName: String = "sagas"
) : SagaStorage {

    companion object {
        private val logger = Logger.default()
    }

    private val storage = LmdbKeyValueStorage(env, sagaSerializer, databaseName)
    
    override suspend fun save(sagaId: String, instance: SagaInstance<*>) {
        val sagaName = instance.sagaName
        logger.debug(sagaName, sagaId) { 
            "Saving saga {sagaName} with ID {sagaId}" 
        }
        val persisted = PersistedSagaInstance(
            id = instance.id,
            sagaName = instance.sagaName,
            state = serializeState(instance.state as Any),
            startedAt = instance.startedAt,
            lastUpdatedAt = instance.lastUpdatedAt
        )
        
        storage.put(sagaId, persisted)
    }
    
    override suspend fun load(sagaId: String): SagaInstance<*>? {
        val persisted = storage.get(sagaId)
        return persisted?.let { deserializeSagaInstance(it) }
    }
    
    override suspend fun remove(sagaId: String) {
        storage.delete(sagaId)
    }
    
    override suspend fun getAllSagaIds(): Set<String> {
        return storage.getAllKeys()
    }
    
    
    /**
     * Serialize saga state to string representation.
     * This is a simplified implementation - in practice, you would need
     * a more sophisticated serialization strategy that can handle different state types.
     */
    private fun serializeState(state: Any): String {
        // For now, just use toString(). In a real implementation,
        // you would need a proper serialization strategy.
        logger.warn { 
            "Using basic serialization for saga state - consider implementing proper serialization" 
        }
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
 * LMDB-based SagaStorage implementation that uses the duks SagaStateSerializer for proper state handling.
 * 
 * @param env The LMDB environment to use (must be already opened)
 * @param persistedSagaSerializer Serializer for converting PersistedSagaInstance to/from bytes
 * @param sagaStateSerializer Serializer from the duks library for proper state handling
 * @param databaseName The name of the database within the LMDB environment (default: "sagas")
 */
class LmdbSagaStorageWithStateSerializer(
    env: Env,
    persistedSagaSerializer: Serializer<PersistedSagaInstance>,
    private val sagaStateSerializer: duks.storage.SagaStateSerializer,
    databaseName: String = "sagas"
) : SagaStorage {
    
    companion object {
        private val logger = Logger.default()
    }
    
    private val storage = LmdbKeyValueStorage(env, persistedSagaSerializer, databaseName)
    
    override suspend fun save(sagaId: String, instance: SagaInstance<*>) {
        val sagaName = instance.sagaName
        logger.debug(sagaName, sagaId) { 
            "Saving saga {sagaName} with ID {sagaId}" 
        }
        val persisted = PersistedSagaInstance(
            id = instance.id,
            sagaName = instance.sagaName,
            state = sagaStateSerializer.serialize(instance.state as Any),
            startedAt = instance.startedAt,
            lastUpdatedAt = instance.lastUpdatedAt
        )
        
        storage.put(sagaId, persisted)
    }
    
    override suspend fun load(sagaId: String): SagaInstance<*>? {
        val persisted = storage.get(sagaId)
        return persisted?.let {
            val state = sagaStateSerializer.deserialize(it.state, it.sagaName)
            SagaInstance(
                id = it.id,
                sagaName = it.sagaName,
                state = state,
                startedAt = it.startedAt,
                lastUpdatedAt = it.lastUpdatedAt
            )
        }
    }
    
    override suspend fun remove(sagaId: String) {
        storage.delete(sagaId)
    }
    
    override suspend fun getAllSagaIds(): Set<String> {
        return storage.getAllKeys()
    }
    
}