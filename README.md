# duks-storage-lmdb

LMDB-based persistent storage implementation for the [duks](https://github.com/crowded-libs/duks) library.

## Overview

This library provides persistent storage implementations for duks using LMDB (Lightning Memory-Mapped Database). LMDB is an ultra-fast, ultra-compact key-value embedded data store that provides:

- ACID transactions
- Excellent performance and reliability
- Small code footprint

## Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.crowded-libs:duks-storage-lmdb:0.1.0")
}
```

## Usage

### Recommended: Shared Environment Pattern

The recommended way to use this library is to create a single LMDB environment and pass it to your storage instances:

```kotlin
import duks.storage.lmdb.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

// Define your state
@Serializable
data class AppState(
    val counter: Int,
    val message: String
)

// Create a serializer
class JsonSerializer<T> : Serializer<T> {
    override fun serialize(value: T): ByteArray {
        return Json.encodeToString(value).encodeToByteArray()
    }
    
    override fun deserialize(bytes: ByteArray): T {
        return Json.decodeFromString(bytes.decodeToString())
    }
}

// Create storage configuration
val config = LmdbStorageConfig(
    path = "/path/to/database",
    mapSize = 10 * 1024 * 1024 // 10MB
)

// Create the LMDB environment
val env = createLmdbEnv(config)

// Create state storage
val stateStorage = LmdbStateStorage(
    env = env,
    serializer = JsonSerializer<AppState>()
)

// Create saga storage
val sagaStorage = LmdbSagaStorage(
    env = env,
    sagaSerializer = JsonSerializer<PersistedSagaInstance>()
)

// Use the storages
val state = AppState(counter = 1, message = "Hello")
stateStorage.save(state)

val sagaInstance = SagaInstance(
    id = "saga-123",
    sagaName = "OrderSaga",
    state = OrderSagaState(orderId = "order-456"),
    startedAt = Clock.System.now(),
    lastUpdatedAt = Clock.System.now()
)
sagaStorage.save(sagaInstance.id, sagaInstance)

// Remember to close the environment when done
env.close()
```

### Multiple Storage Instances

You can create multiple storage instances with different database names within the same environment:

```kotlin
// Create a shared environment
val env = createLmdbEnv(config)

// Create multiple state storages
val userStateStorage = LmdbStateStorage(
    env = env,
    serializer = JsonSerializer<UserState>(),
    databaseName = "user_state"
)

val configStateStorage = LmdbStateStorage(
    env = env,
    serializer = JsonSerializer<ConfigState>(),
    databaseName = "config_state"
)

val sagaStorage = LmdbSagaStorage(
    env = env,
    sagaSerializer = JsonSerializer<PersistedSagaInstance>(),
    databaseName = "sagas"
)

// Or use the general-purpose key-value storage
val kvStorage = LmdbKeyValueStorage(
    env = env,
    databaseName = "custom_data"
)
```

### General-Purpose Key-Value Storage

For custom use cases, you can use the `LmdbKeyValueStorage` class directly:

```kotlin
// Define your custom data type
@Serializable
data class UserData(
    val id: String,
    val name: String,
    val email: String
)

// Create type-safe storage
val kvStorage = LmdbKeyValueStorage(
    env = env,
    serializer = JsonSerializer<UserData>(),
    databaseName = "user_data"
)

// Store and retrieve typed data
val user = UserData("123", "John Doe", "john@example.com")
kvStorage.put("user:123", user)

val retrieved = kvStorage.get("user:123")
val exists = kvStorage.exists("user:123")
kvStorage.delete("user:123")

// Get all keys
val allKeys = kvStorage.getAllKeys()
```

### Serialization

The library uses a `Serializer` interface to allow flexibility in how data is serialized:

```kotlin
interface Serializer<T> {
    fun serialize(value: T): ByteArray
    fun deserialize(bytes: ByteArray): T
}
```

You can implement this interface using any serialization library:
- kotlinx.serialization (recommended)
- Gson
- Jackson
- Protocol Buffers
- etc.

### Configuration Options

```kotlin
data class LmdbStorageConfig(
    val path: String,              // Path to the database directory
    val maxDbs: Int = 10,          // Maximum number of named databases
    val mapSize: Long = 10485760,  // Maximum size of the database (10MB default)
    val maxReaders: Int = 126,     // Maximum number of concurrent readers
    val readOnly: Boolean = false  // Open in read-only mode
)
```

## API Overview

### Core Classes

1. **`LmdbKeyValueStorage<T>`** - Generic key-value storage with built-in serialization
   - Type-safe storage and retrieval
   - Supports any serializable type
   - Full CRUD operations

2. **`LmdbStateStorage<T>`** - StateStorage implementation for duks
   - Single state per database
   - Built on top of `LmdbKeyValueStorage`
   - Uses a fixed key internally

3. **`LmdbSagaStorage`** - SagaStorage implementation for duks
   - Multiple sagas per database
   - Built on top of `LmdbKeyValueStorage`
   - Saga ID as key

### Design Benefits

- **Minimal Redundancy**: All storage classes share the same underlying implementation
- **Type Safety**: Generic typing ensures compile-time safety
- **Flexibility**: Use `LmdbKeyValueStorage` directly for custom storage needs
- **Simplicity**: Each class has a single, focused responsibility

## Multiplatform Support

This library supports the following Kotlin Multiplatform targets:
- JVM ✅
- Android ✅
- iOS (x64, arm64, simulatorArm64) ✅
- WasmJS ✅

### Testing Notes

- **JVM Tests**: Run with `./gradlew :storage-lmdb:jvmTest` ✅
- **iOS Tests**: Run with `./gradlew :storage-lmdb:iosSimulatorArm64Test` (or other iOS targets) ✅
- **Android Tests**: Run with `./gradlew :storage-lmdb:testDebugUnitTest`. Uses kotlin-lmdb 0.3.1+ which includes host platform libraries for testing. ✅
- **WasmJS Tests**: Run with `./gradlew :storage-lmdb:wasmJsNodeTest` ✅

## License

Apache License 2.0