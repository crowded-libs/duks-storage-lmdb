package duks.storage.lmdb

import duks.storage.StateStorage
import lmdb.*

/**
 * LMDB-based implementation of StateStorage for persisting state data.
 * 
 * @param TState The type of state being stored
 * @param env The LMDB environment to use
 * @param serializer Serializer for converting state to/from bytes
 * @param databaseName The name of the database within the LMDB environment (default: "state")
 * @param stateKey The key used to store the state (default: "app_state")
 */
class LmdbStateStorage<TState>(
    private val env: Env,
    private val serializer: Serializer<TState>,
    private val databaseName: String = "state",
    private val stateKey: String = "app_state"
) : StateStorage<TState> {
    
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
    
    override suspend fun save(state: TState) {
        val bytes = serializer.serialize(state)
        env.beginTxn {
            put(dbi, stateKey.encodeToByteArray(), bytes)
            commit()
        }
    }
    
    override suspend fun load(): TState? {
        var result: TState? = null
        env.beginTxn(TxnOption.ReadOnly) {
            val getResult = get(dbi, stateKey.encodeToByteArray())
            if (getResult.resultCode == 0) {
                val bytes = getResult.data.toByteArray()
                if (bytes != null) {
                    result = serializer.deserialize(bytes)
                }
            }
        }
        return result
    }
    
    override suspend fun clear() {
        env.beginTxn {
            delete(dbi, stateKey.encodeToByteArray())
            commit()
        }
    }
    
    override suspend fun exists(): Boolean {
        var exists = false
        env.beginTxn(TxnOption.ReadOnly) {
            val getResult = get(dbi, stateKey.encodeToByteArray())
            exists = getResult.resultCode == 0
        }
        return exists
    }
}

/**
 * Creates an LmdbStateStorage instance with its own environment.
 * This constructor is provided for backward compatibility.
 * Consider using LmdbDuksStorage.createStateStorage() for shared environment usage.
 */
fun <TState> LmdbStateStorage(
    config: LmdbStorageConfig,
    serializer: Serializer<TState>,
    databaseName: String = "state",
    stateKey: String = "app_state"
): LmdbStateStorage<TState> {
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
    
    return LmdbStateStorage(
        env = env,
        serializer = serializer,
        databaseName = databaseName,
        stateKey = stateKey
    )
}