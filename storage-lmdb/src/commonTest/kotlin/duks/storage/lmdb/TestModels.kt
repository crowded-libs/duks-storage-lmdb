package duks.storage.lmdb

import duks.storage.PersistedSagaInstance
import kotlinx.serialization.Serializable

/**
 * Serializable wrapper for PersistedSagaInstance since the original is not @Serializable
 */
@Serializable
data class SerializablePersistedSagaInstance(
    val id: String,
    val sagaName: String,
    val state: String,
    val startedAt: Long,
    val lastUpdatedAt: Long,
    val metadata: Map<String, String> = emptyMap()
) {
    fun toPersistedSagaInstance() = PersistedSagaInstance(
        id = id,
        sagaName = sagaName,
        state = state,
        startedAt = startedAt,
        lastUpdatedAt = lastUpdatedAt,
        metadata = metadata
    )
    
    companion object {
        fun fromPersistedSagaInstance(persisted: PersistedSagaInstance) = SerializablePersistedSagaInstance(
            id = persisted.id,
            sagaName = persisted.sagaName,
            state = persisted.state,
            startedAt = persisted.startedAt,
            lastUpdatedAt = persisted.lastUpdatedAt,
            metadata = persisted.metadata
        )
    }
}

/**
 * Custom serializer that wraps PersistedSagaInstance with SerializablePersistedSagaInstance
 */
class PersistedSagaInstanceSerializer : Serializer<PersistedSagaInstance> {
    private val json = kotlinx.serialization.json.Json
    
    override fun serialize(value: PersistedSagaInstance): ByteArray {
        val serializable = SerializablePersistedSagaInstance.fromPersistedSagaInstance(value)
        return json.encodeToString(SerializablePersistedSagaInstance.serializer(), serializable).encodeToByteArray()
    }
    
    override fun deserialize(bytes: ByteArray): PersistedSagaInstance {
        val serializable = json.decodeFromString(SerializablePersistedSagaInstance.serializer(), bytes.decodeToString())
        return serializable.toPersistedSagaInstance()
    }
}