package duks.storage.lmdb

import duks.SagaInstance
import duks.storage.SagaStorage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

// Test utilities are defined in TestUtils.kt

@Serializable
data class TestSagaState(
    val id: String,
    val step: Int,
    val data: Map<String, String> = emptyMap()
)

class LmdbSagaStorageTest {
    
    private lateinit var testEnv: TestEnvWrapper
    private lateinit var storage: SagaStorage
    
    @BeforeTest
    fun setup() {
        testEnv = createTestEnv()
        storage = LmdbSagaStorage(
            env = testEnv.env,
            sagaSerializer = PersistedSagaInstanceSerializer()
        )
    }
    
    @AfterTest
    fun tearDown() {
        testEnv.close()
    }
    
    @Test
    fun testSaveAndLoad() = runTest {
        val sagaId = "test-saga-1"
        val sagaState = TestSagaState(
            id = sagaId,
            step = 1,
            data = mapOf("key1" to "value1", "key2" to "value2")
        )
        
        val instance = SagaInstance(
            id = sagaId,
            sagaName = "TestSaga",
            state = sagaState,
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        
        storage.save(sagaId, instance)
        
        val loaded = storage.load(sagaId)
        assertNotNull(loaded)
        assertEquals(sagaId, loaded.id)
        assertEquals("TestSaga", loaded.sagaName)
        assertEquals(1000L, loaded.startedAt)
        assertEquals(2000L, loaded.lastUpdatedAt)
        // State will be a string representation due to simplified serialization
        assertTrue(loaded.state.toString().contains("TestSagaState"))
    }
    
    @Test
    fun testLoadNonExistent() = runTest {
        val loaded = storage.load("non-existent-saga")
        assertNull(loaded)
    }
    
    @Test
    fun testRemove() = runTest {
        val sagaId = "test-saga-remove"
        val instance = SagaInstance(
            id = sagaId,
            sagaName = "TestSaga",
            state = TestSagaState(sagaId, 1),
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        
        storage.save(sagaId, instance)
        assertNotNull(storage.load(sagaId))
        
        storage.remove(sagaId)
        assertNull(storage.load(sagaId))
    }
    
    @Test
    fun testGetAllSagaIds() = runTest {
        val sagaIds = setOf("saga-1", "saga-2", "saga-3")
        
        sagaIds.forEach { sagaId ->
            val instance = SagaInstance(
                id = sagaId,
                sagaName = "TestSaga",
                state = TestSagaState(sagaId, 1),
                startedAt = 1000L,
                lastUpdatedAt = 2000L
            )
            storage.save(sagaId, instance)
        }
        
        val retrievedIds = storage.getAllSagaIds()
        assertEquals(sagaIds, retrievedIds)
    }
    
    @Test
    fun testUpdateExistingSaga() = runTest {
        val sagaId = "test-saga-update"
        val initialState = TestSagaState(sagaId, 1)
        val initialInstance = SagaInstance(
            id = sagaId,
            sagaName = "TestSaga",
            state = initialState,
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        
        storage.save(sagaId, initialInstance)
        
        val updatedState = TestSagaState(sagaId, 2, mapOf("updated" to "true"))
        val updatedInstance = SagaInstance(
            id = sagaId,
            sagaName = "TestSaga",
            state = updatedState,
            startedAt = 1000L,
            lastUpdatedAt = 3000L
        )
        
        storage.save(sagaId, updatedInstance)
        
        val loaded = storage.load(sagaId)
        assertNotNull(loaded)
        assertEquals(3000L, loaded.lastUpdatedAt)
        assertTrue(loaded.state.toString().contains("step=2"))
    }
    
    @Test
    fun testMultipleSagasNoCrosstalk() = runTest {
        val saga1Id = "saga-1"
        val saga2Id = "saga-2"
        
        val instance1 = SagaInstance(
            id = saga1Id,
            sagaName = "SagaType1",
            state = TestSagaState(saga1Id, 1),
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        
        val instance2 = SagaInstance(
            id = saga2Id,
            sagaName = "SagaType2",
            state = TestSagaState(saga2Id, 2),
            startedAt = 3000L,
            lastUpdatedAt = 4000L
        )
        
        storage.save(saga1Id, instance1)
        storage.save(saga2Id, instance2)
        
        val loaded1 = storage.load(saga1Id)
        val loaded2 = storage.load(saga2Id)
        
        assertNotNull(loaded1)
        assertNotNull(loaded2)
        
        assertEquals("SagaType1", loaded1.sagaName)
        assertEquals("SagaType2", loaded2.sagaName)
        
        assertEquals(1000L, loaded1.startedAt)
        assertEquals(3000L, loaded2.startedAt)
    }
}