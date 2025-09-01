# MLS Messenger Sample

A sample messenger application with Message Layer Security (MLS) implementation using AWS MLS-rs library and Kotlin Multiplatform.

## Project Structure

```
mls-messenger-sample/
├── shared/                    # Common data models and utilities
├── native/                    # Rust MLS-rs library with UniFFI bindings
├── desktop-client/            # Compose for Desktop client application
├── backend/                   # Ktor backend server with event-driven architecture
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

The backend will start on `http://localhost:8080`

### 4. Run Desktop Clients
```bash
# Terminal 1 - First client instance
./gradlew :desktop-client:run

# Terminal 2 - Second client instance  
./gradlew :desktop-client:run
```

## Features

### ✅ Implemented Features

- **MLS Security**: End-to-end encryption using AWS MLS-rs library
- **Event-Driven Architecture**: Real-time updates via server events
- **Group Management**: Create and join secure groups
- **Join Requests**: Request to join groups with approval workflow
- **Message Encryption**: Secure message sending and receiving
- **User Status Tracking**: Track user membership status in groups
- **Modern UI**: Compose for Desktop with Material 3 design

### 🔧 Technical Features

- **UniFFI Integration**: Rust library bindings for Kotlin
- **Ktor Backend**: RESTful API with in-memory storage
- **StateFlow**: Reactive UI state management
- **Coroutines**: Asynchronous operations and polling
- **Event Polling**: Client-side event processing for real-time updates

## Architecture

### Event-Driven System

The application uses an event-driven architecture where:

1. **Server Events**: Backend generates events for various actions
2. **Client Polling**: Clients poll for events every 2 seconds
3. **State Synchronization**: Client state is updated based on received events

### Event Types

- `GROUP_CREATED`: When a new group is created
- `JOIN_REQUESTED`: When someone requests to join a group
- `JOIN_APPROVED`: When a join request is approved
- `JOIN_DECLINED`: When a join request is declined
- `MESSAGE_SENT`: When a message is sent to a group

### Data Flow

1. **Group Creation**: User creates group → Server generates `GROUP_CREATED` event
2. **Join Request**: User requests to join → Server generates `JOIN_REQUESTED` event
3. **Join Approval**: Group member approves → Server generates `JOIN_APPROVED` event with MLS data
4. **Message Sending**: User sends message → Server generates `MESSAGE_SENT` event

## API Endpoints

### Backend API (`http://localhost:8080`)

- `GET /api/chats` - Get all chats
- `POST /api/chats` - Create a new chat
- `POST /api/chats/{chatId}/messages` - Send a message
- `GET /api/chats/{chatId}/join-requests` - Get join requests for a chat
- `POST /api/users/{userId}/chats/{chatId}/join-request` - Request to join a chat
- `POST /api/join-requests/{requestId}/accept` - Accept a join request
- `POST /api/join-requests/{requestId}/decline` - Decline a join request
- `GET /api/users/{userId}/chats` - Get chats with user status
- `POST /api/users/{userId}/connect` - Connect user and receive existing groups
- `GET /api/users/{userId}/events` - Get events for a user

## Usage Guide

### 1. Starting the Application

1. Start the backend server
2. Launch desktop client instances
3. Enter a user ID for each client

### 2. Creating Groups

1. Click "Create New Chat" in the left sidebar
2. Enter a group name
3. The creator automatically becomes a member

### 3. Joining Groups

1. Select a group where you're not a member
2. Click "Request to Join" button
3. Group members will see a join request notification
4. Members can approve/decline the request

### 4. Sending Messages

1. Select a group where you're a member
2. Type a message in the input field
3. Press Enter to send
4. Messages are encrypted using MLS

## Development Notes

### Current Implementation

- ✅ MLS-rs integration with UniFFI bindings
- ✅ Event-driven architecture with polling
- ✅ Group creation and management
- ✅ Join request workflow
- ✅ Message encryption/decryption
- ✅ User status tracking
- ✅ Modern Compose UI

### Technical Details

- **MLS Library**: AWS MLS-rs for cryptographic operations
- **Backend Storage**: In-memory storage with `ConcurrentHashMap`
- **Client State**: Local state management with `StateFlow`
- **Event Processing**: Client-side event handling and state updates
- **Message Format**: Base64 encoded MLS messages

### Known Limitations

- In-memory storage (data lost on server restart)
- No user authentication
- No message persistence
- No real-time WebSocket communication (uses polling)

## Next Steps

1. **Persistence**: Implement database storage for messages and groups
2. **Authentication**: Add user authentication and authorization
3. **Real-time**: Replace polling with WebSocket connections
4. **Error Handling**: Improve error handling and validation
5. **Testing**: Add comprehensive unit and integration tests
6. **Mobile**: Extend to Android/iOS platforms
7. **Production**: Add logging, monitoring, and deployment configuration

## Troubleshooting

### Common Issues

1. **Native Library Not Found**: Ensure Rust is installed and run `cargo build --release` in the `native` directory
2. **Port Already in Use**: Change the backend port in `MessengerServer.kt` or kill existing processes
3. **Compilation Errors**: Ensure all dependencies are properly installed and try `./gradlew clean build`

### Debug Information

- Backend logs show server operations and event generation
- Client logs show event processing and MLS operations
- Check console output for detailed error messages
