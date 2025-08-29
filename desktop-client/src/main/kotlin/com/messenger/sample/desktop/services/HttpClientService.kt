package com.messenger.sample.desktop.services

import com.messenger.sample.desktop.ui.createClient
import com.messenger.sample.desktop.ui.models.DesktopUser
import com.messenger.sample.shared.models.ChatGroup
import com.messenger.sample.shared.models.ChatGroupWithUserStatus
import com.messenger.sample.shared.models.ChatMembershipStatus
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.CreateChatRequest
import com.messenger.sample.shared.models.CreateJoinRequestRequest
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
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    // Local state
    private val _chatsWithStatus = MutableStateFlow<List<ChatGroupWithUserStatus>>(emptyList())
    val chatsWithStatus: StateFlow<List<ChatGroupWithUserStatus>> = _chatsWithStatus.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    private val _joinRequests = MutableStateFlow<Map<String, List<JoinRequest>>>(emptyMap())
    val joinRequests: StateFlow<Map<String, List<JoinRequest>>> = _joinRequests.asStateFlow()

    private val _currentDesktopUserId = MutableStateFlow<DesktopUser?>(null)
    val currentDesktopUserId: StateFlow<DesktopUser?> = _currentDesktopUserId.asStateFlow()

    private val groups: ConcurrentHashMap<String, Group> = ConcurrentHashMap()

    // Polling jobs
    private var chatsPollingJob: Job? = null
    private var messagesPollingJob: Job? = null
    private var joinRequestsPollingJob: Job? = null

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
     * Start polling for data from the server.
     */
    private fun startPolling() {
        // Poll chats with user status every 4 seconds
        chatsPollingJob = coroutineScope.launch {
            val mlsClient = currentDesktopUserId.value?.client
            while (isActive && mlsClient != null) {
                try {
                    val userId = _currentDesktopUserId.value?.userId ?: continue
                    val response = httpClient.get("$serverBaseUrl/api/users/$userId/chats") {
                        accept(ContentType.Application.Json)
                    }
                    if (response.status.isSuccess()) {
                        val chatsWithStatus = response.body<List<ChatGroupWithUserStatus>>()
                        _chatsWithStatus.value = chatsWithStatus

                        chatsWithStatus
                            .filter { it.userStatus == ChatMembershipStatus.MEMBER }
                            .forEach {
                                if (groups.get(it.id) == null) {
                                    groups[it.id] = mlsClient.createGroup(it.id.toByteArray())
                                        .also { it.writeToStorage() }
                                }
                            }

                        println("✅ Fetched ${chatsWithStatus.size} chats with user status from server")
                    } else {
                        println("❌ Failed to fetch chats with user status: ${response.status}")
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch chats with user status: ${e.message}")
                }
                delay(4000) // 4 seconds
            }
        }

        // Poll join requests for member chats every 3 seconds
        joinRequestsPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val currentChatsWithStatus = _chatsWithStatus.value
                    val memberChats = currentChatsWithStatus.filter { it.userStatus == ChatMembershipStatus.MEMBER }
                    val newJoinRequests = mutableMapOf<String, List<JoinRequest>>()

                    memberChats.forEach { chat ->
                        try {
                            val response = httpClient.get("$serverBaseUrl/api/chats/${chat.id}/join-requests") {
                                accept(ContentType.Application.Json)
                            }
                            if (response.status.isSuccess()) {
                                val chatJoinRequests = response.body<List<JoinRequest>>()
                                if (chatJoinRequests.isNotEmpty()) {
                                    newJoinRequests[chat.id] = chatJoinRequests
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ Failed to fetch join requests for chat ${chat.id}: ${e.message}")
                        }
                    }

                    _joinRequests.value = newJoinRequests
                    if (newJoinRequests.isNotEmpty()) {
                        println("✅ Fetched join requests for ${newJoinRequests.size} chats from server")
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch join requests: ${e.message}")
                }
                delay(3000) // 3 seconds
            }
        }
    }

    /**
     * Send a message to a specific chat.
     */
    suspend fun sendMessage(chatId: String, messageText: String) {
        try {
            val userId = _currentDesktopUserId.value?.userId ?: "Unknown User"
            val group = groups[chatId] ?: return
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
                println("✅ Message sent to chat $chatId")
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
        return _messages.value[chatId] ?: emptyList()
    }

    /**
     * Get join requests for a specific chat.
     */
    suspend fun getJoinRequestsForChat(chatId: String): List<JoinRequest> {
        return try {
            val response = httpClient.get("$serverBaseUrl/api/chats/$chatId/join-requests") {
                accept(ContentType.Application.Json)
            }
            if (response.status.isSuccess()) {
                val requests = response.body<List<JoinRequest>>()
                _joinRequests.value = _joinRequests.value.toMutableMap().apply {
                    put(chatId, requests)
                }
                requests
            } else {
                println("❌ Failed to fetch join requests: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Failed to fetch join requests: ${e.message}")
            emptyList()
        }
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
     * Mark a chat as read.
     */
    suspend fun markChatAsRead(chatId: String) {
        try {
            val response = httpClient.post("$serverBaseUrl/api/chats/$chatId/mark-read")
            if (response.status.isSuccess()) {
                println("✅ Marked chat $chatId as read")
            } else {
                println("❌ Failed to mark chat as read: ${response.status}")
            }
        } catch (e: Exception) {
            println("❌ Failed to mark chat as read: ${e.message}")
        }
    }

    /**
     * Request to join a chat
     */
    suspend fun requestToJoinChat(chatId: String, keyPackage: String): Boolean {
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
     * Get user status for a specific chat
     */
    suspend fun getUserChatStatus(chatId: String): ChatMembershipStatus {
        return try {
            val userId = _currentDesktopUserId.value?.userId ?: return ChatMembershipStatus.NOT_MEMBER
            val response = httpClient.get("$serverBaseUrl/api/users/$userId/chats/$chatId/status") {
                accept(ContentType.Application.Json)
            }

            if (response.status.isSuccess()) {
                val statusMap = response.body<Map<String, String>>()
                val statusString = statusMap["status"] ?: "NOT_MEMBER"
                ChatMembershipStatus.valueOf(statusString)
            } else {
                println("❌ Failed to get user chat status: ${response.status}")
                ChatMembershipStatus.NOT_MEMBER
            }
        } catch (e: Exception) {
            println("❌ Failed to get user chat status: ${e.message}")
            ChatMembershipStatus.NOT_MEMBER
        }
    }

    /**
     * Stop the service and clean up resources.
     */
    fun stop() {
        chatsPollingJob?.cancel()
        messagesPollingJob?.cancel()
        joinRequestsPollingJob?.cancel()
        coroutineScope.cancel()
        httpClient.close()
        _currentDesktopUserId.value?.client?.destroy()
    }


    suspend fun startPollingMessagesOfChat(chatId: String) {
        messagesPollingJob?.cancelAndJoin()
        messagesPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val currentDesktop = _currentDesktopUserId.value?.userId
                    val newMessages = mutableMapOf<String, List<ChatMessage>>()

                    try {
                        val response = httpClient.get("$serverBaseUrl/api/chats/$chatId/messages") {
                            accept(ContentType.Application.Json)
                        }
                        if (response.status.isSuccess()) {
                            val chatMessages = response.body<List<ChatMessage>>()
                            newMessages[chatId] = chatMessages
                                .map { it.copy(isOwnMessage = currentDesktop == it.id) }
                        }
                    } catch (e: Exception) {
                        println("❌ Failed to fetch messages for chat ${chatId}: ${e.message}")
                    }
                    _messages.value = newMessages
                    println("✅ Fetched messages for ${newMessages.size} chats from server")
                } catch (e: Exception) {
                    println("❌ Failed to fetch messages: ${e.message}")
                }
                delay(2000) // 2 seconds
            }
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
