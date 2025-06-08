package duks.storage.lmdb

import duks.storage.StateStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*
// Test utilities are defined in TestUtils.kt

@Serializable
data class TestState(
    val counter: Int,
    val message: String,
    val items: List<String> = emptyList()
)

class LmdbStateStorageTest {
    
    private lateinit var testStorage: TestStorageWrapper
    private lateinit var storage: StateStorage<TestState>
    
    @BeforeTest
    fun setup() {
        testStorage = createTestStorage()
        storage = testStorage.storage.createStateStorage(
            serializer = jsonSerializer<TestState>()
        )
    }
    
    @AfterTest
    fun tearDown() {
        testStorage.close()
    }
    
    @Test
    fun testSaveAndLoad() = runTest {
        val state = TestState(
            counter = 42,
            message = "Hello LMDB",
            items = listOf("item1", "item2", "item3")
        )
        
        storage.save(state)
        
        val loaded = storage.load()
        assertNotNull(loaded)
        assertEquals(state, loaded)
    }
    
    @Test
    fun testLoadNonExistent() = runTest {
        val loaded = storage.load()
        assertNull(loaded)
    }
    
    @Test
    fun testExists() = runTest {
        assertFalse(storage.exists())
        
        val state = TestState(counter = 1, message = "test")
        storage.save(state)
        
        assertTrue(storage.exists())
    }
    
    @Test
    fun testClear() = runTest {
        val state = TestState(counter = 10, message = "clear test")
        storage.save(state)
        
        assertTrue(storage.exists())
        assertNotNull(storage.load())
        
        storage.clear()
        
        assertFalse(storage.exists())
        assertNull(storage.load())
    }
    
    @Test
    fun testOverwrite() = runTest {
        val state1 = TestState(counter = 1, message = "first")
        storage.save(state1)
        
        val state2 = TestState(counter = 2, message = "second", items = listOf("a", "b"))
        storage.save(state2)
        
        val loaded = storage.load()
        assertEquals(state2, loaded)
    }
    
    @Test
    fun testLargeState() = runTest {
        val largeItems = List(1000) { "item-$it-with-some-longer-content-to-increase-size" }
        val state = TestState(
            counter = 999,
            message = "Large state test",
            items = largeItems
        )
        
        storage.save(state)
        
        val loaded = storage.load()
        assertEquals(state, loaded)
        assertEquals(1000, loaded?.items?.size)
    }
    
    @Test  
    fun testStorageIsASingleton() = runTest {
        // This test demonstrates that LMDB storage instances should be used
        // as singletons. LMDB doesn't support multiple write environments
        // for the same path, so in a real application, you would create
        // a single storage instance and share it across your application.
        
        val state = TestState(counter = 100, message = "singleton")
        storage.save(state)
        
        // Verify the data was saved
        assertEquals(state, storage.load())
        
        // Update the state
        val updatedState = TestState(counter = 200, message = "updated singleton")
        storage.save(updatedState)
        
        // Verify the update
        assertEquals(updatedState, storage.load())
        
        // The storage instance remains open and can be used throughout
        // the application lifecycle. It should only be closed when the
        // application shuts down.
    }
}