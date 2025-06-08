package duks.storage.lmdb

import duks.storage.StateStorage
import duks.storage.SagaStorage
import duks.storage.PersistedSagaInstance
import duks.storage.SagaStateSerializer
import lmdb.*

/**
 * Central storage manager for LMDB-based duks storage implementations.
 * This class manages a single LMDB environment that is shared between
 * StateStorage and SagaStorage instances.
 * 
 * @param config Configuration for the LMDB environment
 */
class LmdbDuksStorage(
    private val config: LmdbStorageConfig
) {
    
    private val env: Env by lazy {
        Env().apply {
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
    }
    
    /**
     * Create a StateStorage instance that uses the shared LMDB environment.
     * 
     * @param TState The type of state being stored
     * @param serializer Serializer for converting state to/from bytes
     * @param databaseName The name of the database within the LMDB environment (default: "state")
     * @param stateKey The key used to store the state (default: "app_state")
     * @return A StateStorage instance backed by LMDB
     */
    fun <TState> createStateStorage(
        serializer: Serializer<TState>,
        databaseName: String = "state",
        stateKey: String = "app_state"
    ): StateStorage<TState> {
        // Force initialization of the environment
        env
        
        return LmdbStateStorage(
            env = env,
            serializer = serializer,
            databaseName = databaseName,
            stateKey = stateKey
        )
    }
    
    /**
     * Create a SagaStorage instance that uses the shared LMDB environment.
     * This version uses simple string serialization for saga state.
     * 
     * @param sagaSerializer Serializer for converting PersistedSagaInstance to/from bytes
     * @param databaseName The name of the database within the LMDB environment (default: "sagas")
     * @return A SagaStorage instance backed by LMDB
     */
    fun createSagaStorage(
        sagaSerializer: Serializer<PersistedSagaInstance>,
        databaseName: String = "sagas"
    ): SagaStorage {
        // Force initialization of the environment
        env
        
        return LmdbSagaStorage(
            env = env,
            sagaSerializer = sagaSerializer,
            databaseName = databaseName
        )
    }
    
    /**
     * Create a SagaStorage instance that uses the shared LMDB environment
     * with proper saga state serialization.
     * 
     * @param persistedSagaSerializer Serializer for converting PersistedSagaInstance to/from bytes
     * @param sagaStateSerializer Serializer from the duks library for proper state handling
     * @param databaseName The name of the database within the LMDB environment (default: "sagas")
     * @return A SagaStorage instance backed by LMDB
     */
    fun createSagaStorage(
        persistedSagaSerializer: Serializer<PersistedSagaInstance>,
        sagaStateSerializer: SagaStateSerializer,
        databaseName: String = "sagas"
    ): SagaStorage {
        // Force initialization of the environment
        env
        
        return LmdbSagaStorageWithStateSerializer(
            env = env,
            persistedSagaSerializer = persistedSagaSerializer,
            sagaStateSerializer = sagaStateSerializer,
            databaseName = databaseName
        )
    }
    
    /**
     * Close the LMDB environment. Should be called when the storage is no longer needed.
     * This will close the shared environment, so all storage instances created from
     * this manager will become unusable.
     */
    fun close() {
        env.close()
    }
}