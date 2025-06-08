package duks.storage.lmdb

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Test implementation of Serializer using kotlinx.serialization JSON format.
 * 
 * @param T The type to serialize/deserialize
 * @param json The Json instance to use for serialization
 * @param serializer The KSerializer for type T
 */
class JsonSerializer<T>(
    private val json: Json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    },
    private val serializer: KSerializer<T>
) : Serializer<T> {
    
    override fun serialize(value: T): ByteArray {
        val jsonString = json.encodeToString(serializer, value)
        return jsonString.encodeToByteArray()
    }
    
    override fun deserialize(bytes: ByteArray): T {
        val jsonString = bytes.decodeToString()
        return json.decodeFromString(serializer, jsonString)
    }
}

/**
 * Convenience function to create a JsonSerializer with default Json configuration.
 */
inline fun <reified T> jsonSerializer(
    json: Json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }
): JsonSerializer<T> {
    return JsonSerializer(json, kotlinx.serialization.serializer())
}