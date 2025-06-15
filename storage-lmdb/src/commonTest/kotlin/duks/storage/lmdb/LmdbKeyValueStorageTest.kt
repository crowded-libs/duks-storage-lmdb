package duks.storage.lmdb

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

@Serializable
data class TestData(
    val id: String,
    val name: String,
    val values: List<Int> = emptyList()
)

class LmdbKeyValueStorageTest {
    
    private lateinit var testEnv: TestEnvWrapper
    private lateinit var storage: LmdbKeyValueStorage<TestData>
    
    @BeforeTest
    fun setup() {
        testEnv = createTestEnv()
        storage = LmdbKeyValueStorage(
            env = testEnv.env,
            serializer = jsonSerializer<TestData>(),
            databaseName = "test_kv"
        )
    }
    
    @AfterTest
    fun tearDown() {
        testEnv.close()
    }
    
    @Test
    fun testPutAndGet() = runTest {
        val key = "test-key-1"
        val data = TestData("1", "Test Item", listOf(1, 2, 3))
        
        storage.put(key, data)
        
        val retrieved = storage.get(key)
        assertNotNull(retrieved)
        assertEquals(data, retrieved)
    }
    
    @Test
    fun testGetNonExistent() = runTest {
        val retrieved = storage.get("non-existent")
        assertNull(retrieved)
    }
    
    @Test
    fun testDelete() = runTest {
        val key = "test-delete"
        val data = TestData("2", "Delete Me")
        
        storage.put(key, data)
        assertTrue(storage.exists(key))
        
        storage.delete(key)
        assertFalse(storage.exists(key))
        assertNull(storage.get(key))
    }
    
    @Test
    fun testExists() = runTest {
        val key = "test-exists"
        
        assertFalse(storage.exists(key))
        
        storage.put(key, TestData("3", "Exists"))
        assertTrue(storage.exists(key))
    }
    
    @Test
    fun testGetAllKeys() = runTest {
        val keys = setOf("key1", "key2", "key3")
        
        keys.forEachIndexed { index, key ->
            storage.put(key, TestData("$index", "Item $index"))
        }
        
        val allKeys = storage.getAllKeys()
        assertEquals(keys, allKeys)
    }
    
    @Test
    fun testUpdate() = runTest {
        val key = "update-key"
        val original = TestData("4", "Original")
        val updated = TestData("4", "Updated", listOf(10, 20))
        
        storage.put(key, original)
        assertEquals(original, storage.get(key))
        
        storage.put(key, updated)
        assertEquals(updated, storage.get(key))
    }
    
    @Test
    fun testMultipleStoragesNoCrosstalk() = runTest {
        // Create another storage with different type
        val stringStorage = LmdbKeyValueStorage(
            env = testEnv.env,
            serializer = object : Serializer<String> {
                override fun serialize(value: String) = value.encodeToByteArray()
                override fun deserialize(bytes: ByteArray) = bytes.decodeToString()
            },
            databaseName = "strings"
        )
        
        // Put data in both storages
        storage.put("shared-key", TestData("5", "Data Storage"))
        stringStorage.put("shared-key", "String Storage")
        
        // Verify no crosstalk
        assertEquals("Data Storage", storage.get("shared-key")?.name)
        assertEquals("String Storage", stringStorage.get("shared-key"))
        
        // Keys are isolated by database
        assertEquals(setOf("shared-key"), storage.getAllKeys())
        assertEquals(setOf("shared-key"), stringStorage.getAllKeys())
    }
}