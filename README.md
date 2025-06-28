# duks-storage-lmdb

LMDB-based persistent storage implementation for the [duks](https://github.com/crowded-libs/duks) library.

[![Build](https://github.com/crowded-libs/duks-storage-lmdb/actions/workflows/build.yml/badge.svg)](https://github.com/crowded-libs/duks-storage-lmdb/actions/workflows/build.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.crowded-libs/duks-storage-lmdb.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.crowded-libs%22%20AND%20a:%22duks-storage-lmdb%22)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


## Overview

This library provides persistent storage implementations for duks using LMDB (Lightning Memory-Mapped Database). LMDB is an ultra-fast, ultra-compact key-value embedded data store that provides:

- ACID transactions
- Excellent performance and reliability
- Small code footprint

## Installation

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.crowded-libs:duks-storage-lmdb:0.1.1")
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

## License

Apache License 2.0