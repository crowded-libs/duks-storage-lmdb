package duks.storage.lmdb

/**
 * Interface for serializing and deserializing objects to/from byte arrays.
 * This allows consumers to use their preferred serialization library
 * (e.g., kotlinx.serialization, Moshi, Jackson, etc.)
 * 
 * @param T The type of object to serialize/deserialize
 */
interface Serializer<T> {
    /**
     * Serialize an object to a byte array.
     * 
     * @param value The object to serialize
     * @return The serialized bytes
     * @throws Exception if serialization fails
     */
    fun serialize(value: T): ByteArray
    
    /**
     * Deserialize a byte array back to an object.
     * 
     * @param bytes The bytes to deserialize
     * @return The deserialized object
     * @throws Exception if deserialization fails
     */
    fun deserialize(bytes: ByteArray): T
}