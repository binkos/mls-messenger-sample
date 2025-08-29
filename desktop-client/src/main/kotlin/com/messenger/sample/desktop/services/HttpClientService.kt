package com.messenger.sample.desktop.services

import com.messenger.sample.desktop.ui.createClient
import com.messenger.sample.desktop.ui.models.DesktopUser
import com.messenger.sample.shared.models.ChatGroup
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.CreateChatRequest
import com.messenger.sample.shared.models.CreateJoinRequestRequest
import com.messenger.sample.shared.models.Event
import com.messenger.sample.shared.models.EventType
import com.messenger.sample.shared.models.JoinRequest
import com.messenger.sample.shared.models.SendMessageRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import uniffi.mls_rs_uniffi.FfiConverterTypeMessage
import uniffi.mls_rs_uniffi.Group
import uniffi.mls_rs_uniffi.Message

/**
 * HTTP client service that communicates with the backend API.
 */
class HttpClientService {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // Local state - managed by events
    private val _chats = MutableStateFlow<List<ChatGroup>>(emptyList())
    val chats: StateFlow<List<ChatGroup>> = _chats.asStateFlow()

    val messageMutex = Mutex()
    private val _messages = HashMap<String, List<ChatMessage>>()
    private val messagesTriggerFlow = MutableStateFlow(0)
    val messages: Flow<Map<String, List<ChatMessage>>> = messagesTriggerFlow.map { _messages }

    private val _joinRequests = MutableStateFlow<Map<String, List<JoinRequest>>>(emptyMap())
    val joinRequests: StateFlow<Map<String, List<JoinRequest>>> = _joinRequests.asStateFlow()

    private val _currentDesktopUserId = MutableStateFlow<DesktopUser?>(null)
    val currentDesktopUserId: StateFlow<DesktopUser?> = _currentDesktopUserId.asStateFlow()

    private val _lastProcessedEventId = MutableStateFlow<String?>(null)
    val lastProcessedEventId: StateFlow<String?> = _lastProcessedEventId.asStateFlow()

    private val groups: ConcurrentHashMap<String, Group> = ConcurrentHashMap()

    // Polling jobs
    private var eventsPollingJob: Job? = null

    // Server configuration
    private val serverBaseUrl = "http://localhost:8080"

    /**
     * Initialize the client service with a user ID.
     */
    fun initialize(userId: String) {
        _currentDesktopUserId.value = DesktopUser(userId = userId, client = createClient(userId))
        startPolling()
    }

    /**
     * Start polling for events from the server.
     */
    private fun startPolling() {
        // Poll events every 2 seconds
        eventsPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val userId = _currentDesktopUserId.value?.userId
                    if (userId != null) {
                        val lastEventId = _lastProcessedEventId.value
                        val response = httpClient.get("$serverBaseUrl/api/users/$userId/events") {
                            accept(ContentType.Application.Json)
                            if (lastEventId != null) {
                                url {
                                    parameters.append("since", lastEventId)
                                }
                            }
                        }
                        if (response.status.isSuccess()) {
                            val newEvents = response.body<List<Event>>()
                            if (newEvents.isNotEmpty()) {
                                processEvents(newEvents)
                                _lastProcessedEventId.value = newEvents.last().id
                                println("✅ Processed ${newEvents.size} new events")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch events: ${e.message}")
                }
                delay(2000) // 2 seconds
            }
        }
    }

    /**
     * Send a message to a specific chat.
     */
    suspend fun sendMessage(chatId: String, messageText: String) {
        try {
            val userId = _currentDesktopUserId.value?.userId ?: "Unknown User"
            val group = groups[chatId] ?: throw Exception("Group not found for chat $chatId")
            val message = group.encryptApplicationMessage(messageText.toByteArray()).use { serializeMessage(it) }
            val request = SendMessageRequest(
                userName = userId,
                message = Base64.getEncoder().encodeToString(message)
            )

            val response = httpClient.post("$serverBaseUrl/api/chats/$chatId/messages") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val createdMessage = response.body<ChatMessage>()
                messageMutex.withLock {
                    // Add the message to local state
                    val currentMessages = _messages[chatId] ?: emptyList()
                    val updatedMessages = currentMessages + createdMessage.copy(message = messageText)
                    _messages[chatId] = updatedMessages
                }
                messagesTriggerFlow.value = messagesTriggerFlow.value + 1
                println("✅ Message sent to chat $chatId: ${createdMessage.message}")
            } else {
                println("❌ Failed to send message: ${response.status}")
                throw Exception("Failed to send message: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Failed to send message: ${e.message}")
            throw e
        }
    }

    /**
     * Create a new chat.
     */
    suspend fun createChat(chatName: String): ChatGroup {
        return try {
            val request = CreateChatRequest(chatName)
            val userId = _currentDesktopUserId.value?.userId

            val response = httpClient.post("$serverBaseUrl/api/chats") {
                contentType(ContentType.Application.Json)
                setBody(request)
                // Add user ID header so backend can mark creator as member
                if (userId != null) {
                    header("X-User-ID", userId)
                }
            }

            if (response.status.isSuccess()) {
                val newChat = response.body<ChatGroup>()
                println("✅ Created new chat: ${newChat.name}")
                newChat
            } else {
                println("❌ Failed to create chat: ${response.status}")
                throw Exception("Failed to create chat: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Failed to create chat: ${e.message}")
            throw e
        }
    }

    /**
     * Get messages for a specific chat.
     */
    fun getMessagesForChat(chatId: String): List<ChatMessage> {
        return _messages[chatId] ?: emptyList()
    }

    /**
     * Accept a join request.
     */
    suspend fun acceptJoinRequest(joinRequest: JoinRequest) {
        try {
            val response = httpClient.post("$serverBaseUrl/api/join-requests/${joinRequest.id}/accept")
            if (response.status.isSuccess()) {
                println("✅ Accepted join request from ${joinRequest.userName}")

                // Update local state
                val currentRequests = _joinRequests.value.toMutableMap()
                currentRequests[joinRequest.groupId] =
                    currentRequests[joinRequest.groupId]?.filter { it.id != joinRequest.id } ?: emptyList()
                _joinRequests.value = currentRequests
            } else {
                println("❌ Failed to accept join request: ${response.status}")
                throw Exception("Failed to accept join request: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Failed to accept join request: ${e.message}")
            throw e
        }
    }

    /**
     * Decline a join request.
     */
    suspend fun declineJoinRequest(joinRequest: JoinRequest) {
        try {
            val response = httpClient.post("$serverBaseUrl/api/join-requests/${joinRequest.id}/decline")
            if (response.status.isSuccess()) {
                println("❌ Declined join request from ${joinRequest.userName}")

                // Update local state
                val currentRequests = _joinRequests.value.toMutableMap()
                currentRequests[joinRequest.groupId] =
                    currentRequests[joinRequest.groupId]?.filter { it.id != joinRequest.id } ?: emptyList()
                _joinRequests.value = currentRequests
            } else {
                println("❌ Failed to decline join request: ${response.status}")
                throw Exception("Failed to decline join request: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Failed to decline join request: ${e.message}")
            throw e
        }
    }

    /**
     * Request to join a chat
     */
    suspend fun requestToJoinChat(chatId: String): Boolean {
        return try {
            val userId = _currentDesktopUserId.value?.userId ?: return false
            val keyPackage = _currentDesktopUserId.value?.client?.generateKeyPackageMessage() ?: return false
            val bytes = serializeMessage(keyPackage)

            val request = CreateJoinRequestRequest(
                userName = userId,
                keyPackage = Base64.getEncoder().encodeToString(bytes).also { println(it) },
                groupId = chatId
            )

            val response = httpClient.post("$serverBaseUrl/api/users/$userId/chats/$chatId/join-request") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                println("✅ Join request sent for chat $chatId")
                true
            } else {
                println("❌ Failed to send join request: ${response.status}")
                false
            }
        } catch (e: Exception) {
            println("❌ Failed to send join request: ${e.message}")
            false
        }
    }

    /**
     * Helper function to remove a join request by ID
     */
    private fun removeJoinRequestById(chatId: String, requestId: String) {
        val currentRequests = _joinRequests.value.toMutableMap()
        currentRequests[chatId]?.let { requests ->
            val filteredRequests = requests.filter { it.id != requestId }
            currentRequests[chatId] = filteredRequests.toMutableList()
        }
        _joinRequests.value = currentRequests
    }

    /**
     * Process incoming events and update local state
     */
    private fun processEvents(events: List<Event>) {
        events.forEach { event ->
            when (event.type) {
                EventType.GROUP_CREATED -> {
                    val chatId = event.chatId ?: return@forEach
                    val chatName = event.data["name"] ?: return@forEach
                    val newChat = ChatGroup(
                        id = chatId,
                        name = chatName,
                        lastMessage = null,
                        lastMessageTime = null,
                        unreadCount = 0
                    )
                    val currentChats = _chats.value.toMutableList()
                    currentChats.add(newChat)
                    _chats.value = currentChats
                    println("✅ Added new chat: $chatName")
                }

                EventType.JOIN_REQUESTED -> {
                    val chatId = event.chatId ?: return@forEach
                    val requesterName = event.data["requesterName"] ?: return@forEach
                    val requestId = event.data["requestId"] ?: return@forEach
                    val joinRequest = JoinRequest(
                        id = requestId,
                        userName = requesterName,
                        keyPackage = event.data["keyPackage"] ?: "",
                        groupId = chatId,
                        timestamp = event.timestamp
                    )

                    // Add join request to the map
                    val currentRequests = _joinRequests.value.toMutableMap()
                    val existingRequests = currentRequests[chatId] ?: mutableListOf<JoinRequest>()
                    val updatedRequests = existingRequests.toMutableList()
                    updatedRequests.add(joinRequest)
                    currentRequests[chatId] = updatedRequests
                    _joinRequests.value = currentRequests

                    println("✅ Added join request from $requesterName")
                }

                EventType.JOIN_APPROVED -> {
                    val chatId = event.chatId ?: return@forEach
                    val approvedUserName = event.data["approvedUserName"] ?: return@forEach
                    val requestId = event.data["requestId"] ?: return@forEach

                    // Remove the join request
                    removeJoinRequestById(chatId, requestId)

                    println("✅ Join request approved for $approvedUserName")
                }

                EventType.JOIN_DECLINED -> {
                    val chatId = event.chatId ?: return@forEach
                    val declinedUserName = event.data["declinedUserName"] ?: return@forEach
                    val requestId = event.data["requestId"] ?: return@forEach

                    // Remove the join request
                    removeJoinRequestById(chatId, requestId)

                    println("❌ Join request declined for $declinedUserName")
                }

                EventType.USER_JOINED -> {
                    println("✅ User joined: ${event.data["userName"]}")
                }

                EventType.USER_LEFT -> {
                    println("👋 User left: ${event.data["userName"]}")
                }
            }
        }
    }

    /**
     * Stop the service and clean up resources.
     */
    fun stop() {
        eventsPollingJob?.cancel()
        coroutineScope.cancel()
        httpClient.close()
        _currentDesktopUserId.value?.client?.destroy()
    }


    suspend fun startPollingMessagesOfChat(chatId: String) {
        // For now, we'll fetch messages directly without polling
        try {
            val response = httpClient.get("$serverBaseUrl/api/chats/$chatId/messages") {
                accept(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                messageMutex.withLock {
                    val chatMessages = response.body<List<ChatMessage>>()
                    val currentMessages = _messages.toMutableMap()
                    currentMessages[chatId] = chatMessages
                    println("✅ Fetched ${chatMessages.size} messages for chat $chatId")
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to fetch messages for chat $chatId: ${e.message}")
        }
    }

    private fun serializeMessage(message: Message): ByteArray {
        val bufferSize = FfiConverterTypeMessage.allocationSize(message)
        val buffer = ByteBuffer.allocateDirect(bufferSize.toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        FfiConverterTypeMessage.write(message, buffer)

        val position = buffer.position()
        val bytes = ByteArray(position)
        buffer.rewind()
        buffer.get(bytes)

        return bytes
    }

    private fun deserializeMessage(bytes: ByteArray): Message {
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return FfiConverterTypeMessage.read(buffer)
    }
}
