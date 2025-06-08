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

The recommended way to use this library is through the `LmdbDuksStorage` class, which manages a single LMDB environment shared between all storage instances:

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

// Create the central storage manager
val duksStorage = LmdbDuksStorage(config)

// Create state storage
val stateStorage = duksStorage.createStateStorage(
    serializer = JsonSerializer<AppState>()
)

// Create saga storage
val sagaStorage = duksStorage.createSagaStorage(
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

// Remember to close the shared environment when done
duksStorage.close()
```

### Multiple Storage Instances

You can create multiple storage instances with different database names within the same environment:

```kotlin
// Create multiple state storages
val userStateStorage = duksStorage.createStateStorage(
    serializer = JsonSerializer<UserState>(),
    databaseName = "user_state"
)

val configStateStorage = duksStorage.createStateStorage(
    serializer = JsonSerializer<ConfigState>(),
    databaseName = "config_state"
)

val sagaStorage = duksStorage.createSagaStorage(
    sagaSerializer = JsonSerializer<PersistedSagaInstance>(),
    databaseName = "sagas"
)
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