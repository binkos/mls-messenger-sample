# MLS Messenger Sample

A sample messenger application with Message Layer Security (MLS) implementation using OpenMLS (Rust) and Kotlin.

## Project Structure

```
mls-messenger-sample/
├── shared/                    # Common data models and utilities
├── native/                    # Rust OpenMLS library with FFI bindings
├── native-wrapper/            # JNI wrapper for native library
├── backend/                   # Ktor backend server
├── desktop-client/            # Compose for Desktop client application
└── build.gradle.kts          # Root build configuration
```

## Prerequisites

- Java 17 or higher
- Rust toolchain (for native compilation)
- macOS (currently configured for macOS)

**Note**: The project includes the Gradle wrapper (`gradlew`), so you don't need to install Gradle separately.

## Setup Instructions

### 1. Install Rust
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source ~/.cargo/env

# Verify installation
cargo --version
rustc --version
```

### 2. Build the Project
```bash
# Build the entire project (includes native library)
./gradlew build

# Or build step by step:
# 1. Build native library first
cd native
cargo build --release
cd ..

# 2. Build the entire project
./gradlew build
```

### 3. Run the Backend
```bash
./gradlew :backend:run
```

### 4. Run Desktop Clients
```bash
# Terminal 1 - First client instance
./gradlew :desktop-client:run

# Terminal 2 - Second client instance  
./gradlew :desktop-client:run
```

## Features

- **MLS Security**: End-to-end encryption using OpenMLS
- **Group Messaging**: Create and join secure groups
- **Real-time Updates**: Polling-based message synchronization
- **Cross-platform**: Native compilation for different operating systems

## Architecture

- **Native Layer**: OpenMLS Rust library with C FFI
- **JNI Wrapper**: Kotlin interface to native library
- **Backend**: Ktor server with REST API
- **Client**: Compose for Desktop application
- **Shared**: Common data models and utilities

## Development Notes

- The native library currently contains placeholder implementations
- MLS group operations need to be implemented using actual OpenMLS APIs
- Message encryption/decryption will be handled by the native library
- Backend provides simple in-memory storage for development

## Next Steps

1. Implement actual OpenMLS functionality in Rust
2. Add proper error handling and validation
3. Implement message persistence
4. Add user authentication
5. Implement real-time messaging (WebSockets)
6. Add proper testing
