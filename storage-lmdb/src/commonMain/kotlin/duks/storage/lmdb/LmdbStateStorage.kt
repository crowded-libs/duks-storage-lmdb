package duks.storage.lmdb

import duks.storage.StateStorage
import lmdb.Env

/**
 * LMDB-based implementation of StateStorage for persisting state data.
 * 
 * @param TState The type of state being stored
 * @param env The LMDB environment to use (must be already opened)
 * @param serializer Serializer for converting state to/from bytes
 * @param databaseName The name of the database within the LMDB environment (default: "state")
 * @param stateKey The key used to store the state (default: "app_state")
 */
class LmdbStateStorage<TState>(
    env: Env,
    serializer: Serializer<TState>,
    databaseName: String = "state",
    private val stateKey: String = "app_state"
) : StateStorage<TState> {
    
    private val storage = LmdbKeyValueStorage(env, serializer, databaseName)
    
    override suspend fun save(state: TState) {
        storage.put(stateKey, state)
    }
    
    override suspend fun load(): TState? {
        return storage.get(stateKey)
    }
    
    override suspend fun clear() {
        storage.delete(stateKey)
    }
    
    override suspend fun exists(): Boolean {
        return storage.exists(stateKey)
    }
}