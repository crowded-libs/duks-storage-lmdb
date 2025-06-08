package duks.storage.lmdb

import duks.SagaInstance
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.*

// Test utilities are defined in TestUtils.kt

@Serializable
data class TestAppState(
    val version: Int,
    val data: String
)

@Serializable
data class TestSagaData(
    val orderId: String,
    val status: String
)

class LmdbDuksStorageTest {
    
    private lateinit var testStorage: TestStorageWrapper
    private lateinit var duksStorage: LmdbDuksStorage
    
    @BeforeTest
    fun setup() {
        testStorage = createTestStorage()
        duksStorage = testStorage.storage
    }
    
    @AfterTest
    fun tearDown() {
        testStorage.close()
    }
    
    @Test
    fun testSharedEnvironment() = runTest {
        // Create both storage types from the same LmdbDuksStorage instance
        val stateStorage = duksStorage.createStateStorage(
            serializer = jsonSerializer<TestAppState>()
        )
        
        val sagaStorage = duksStorage.createSagaStorage(
            sagaSerializer = PersistedSagaInstanceSerializer()
        )
        
        // Save state data
        val appState = TestAppState(version = 1, data = "shared environment test")
        stateStorage.save(appState)
        
        // Save saga data
        val sagaId = "saga-shared-1"
        val sagaInstance = SagaInstance(
            id = sagaId,
            sagaName = "TestSharedSaga",
            state = TestSagaData(orderId = "order-123", status = "processing"),
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        sagaStorage.save(sagaId, sagaInstance)
        
        // Verify both can read their data
        val loadedState = stateStorage.load()
        assertNotNull(loadedState)
        assertEquals(appState, loadedState)
        
        val loadedSaga = sagaStorage.load(sagaId)
        assertNotNull(loadedSaga)
        assertEquals(sagaId, loadedSaga.id)
        assertEquals("TestSharedSaga", loadedSaga.sagaName)
        
        // Both storages are using the same environment, so closing duksStorage
        // will close the shared environment and make both storages unusable
    }
    
    @Test
    fun testMultipleStateStorages() = runTest {
        // Create multiple state storages with different database names
        val userStateStorage = duksStorage.createStateStorage(
            serializer = jsonSerializer<TestAppState>(),
            databaseName = "user_state"
        )
        
        val configStateStorage = duksStorage.createStateStorage(
            serializer = jsonSerializer<TestAppState>(),
            databaseName = "config_state"
        )
        
        // Save different data to each
        val userState = TestAppState(version = 1, data = "user data")
        val configState = TestAppState(version = 2, data = "config data")
        
        userStateStorage.save(userState)
        configStateStorage.save(configState)
        
        // Verify each storage maintains its own data
        assertEquals(userState, userStateStorage.load())
        assertEquals(configState, configStateStorage.load())
    }
    
    @Test
    fun testMultipleSagaStorages() = runTest {
        // Create multiple saga storages with different database names
        val orderSagaStorage = duksStorage.createSagaStorage(
            sagaSerializer = PersistedSagaInstanceSerializer(),
            databaseName = "order_sagas"
        )
        
        val paymentSagaStorage = duksStorage.createSagaStorage(
            sagaSerializer = PersistedSagaInstanceSerializer(),
            databaseName = "payment_sagas"
        )
        
        // Save different sagas to each
        val orderSagaId = "order-saga-1"
        val orderSaga = SagaInstance(
            id = orderSagaId,
            sagaName = "OrderSaga",
            state = TestSagaData(orderId = "order-456", status = "pending"),
            startedAt = 1000L,
            lastUpdatedAt = 2000L
        )
        
        val paymentSagaId = "payment-saga-1"
        val paymentSaga = SagaInstance(
            id = paymentSagaId,
            sagaName = "PaymentSaga",
            state = TestSagaData(orderId = "order-456", status = "processing"),
            startedAt = 3000L,
            lastUpdatedAt = 4000L
        )
        
        orderSagaStorage.save(orderSagaId, orderSaga)
        paymentSagaStorage.save(paymentSagaId, paymentSaga)
        
        // Verify each storage maintains its own data
        val loadedOrderSaga = orderSagaStorage.load(orderSagaId)
        assertNotNull(loadedOrderSaga)
        assertEquals("OrderSaga", loadedOrderSaga.sagaName)
        
        val loadedPaymentSaga = paymentSagaStorage.load(paymentSagaId)
        assertNotNull(loadedPaymentSaga)
        assertEquals("PaymentSaga", loadedPaymentSaga.sagaName)
        
        // Verify each storage only knows about its own sagas
        assertEquals(setOf(orderSagaId), orderSagaStorage.getAllSagaIds())
        assertEquals(setOf(paymentSagaId), paymentSagaStorage.getAllSagaIds())
    }
}