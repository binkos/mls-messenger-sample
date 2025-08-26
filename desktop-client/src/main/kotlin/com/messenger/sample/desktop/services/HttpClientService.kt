package com.messenger.sample.desktop.services

import com.messenger.sample.shared.models.ChatGroup
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.CreateChatRequest
import com.messenger.sample.shared.models.JoinRequest
import com.messenger.sample.shared.models.SendMessageRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * HTTP client service that communicates with the backend API.
 */
class HttpClientService {
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
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
    private val _chats = MutableStateFlow<List<ChatGroup>>(emptyList())
    val chats: StateFlow<List<ChatGroup>> = _chats.asStateFlow()
    
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()
    
    private val _joinRequests = MutableStateFlow<Map<String, List<JoinRequest>>>(emptyMap())
    val joinRequests: StateFlow<Map<String, List<JoinRequest>>> = _joinRequests.asStateFlow()
    
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    // Polling jobs
    private var chatsPollingJob: Job? = null
    private var messagesPollingJob: Job? = null
    
    // Server configuration
    private val serverBaseUrl = "http://localhost:8080"
    
    /**
     * Initialize the client service with a user ID.
     */
    fun initialize(userId: String) {
        _currentUserId.value = userId
        startPolling()
    }
    
    /**
     * Start polling for data from the server.
     */
    private fun startPolling() {
        // Poll chats every 4 seconds
        chatsPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val response = httpClient.get("$serverBaseUrl/api/chats") {
                        accept(ContentType.Application.Json)
                    }
                    if (response.status.isSuccess()) {
                        val serverChats = response.body<List<ChatGroup>>()
                        _chats.value = serverChats
                        println("✅ Fetched ${serverChats.size} chats from server")
                    } else {
                        println("❌ Failed to fetch chats: ${response.status}")
                    }
                } catch (e: Exception) {
                    println("❌ Failed to fetch chats: ${e.message}")
                }
                delay(4000) // 4 seconds
            }
        }
        
        // Poll messages every 2 seconds
        messagesPollingJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val currentChats = _chats.value
                    val newMessages = mutableMapOf<String, List<ChatMessage>>()
                    
                    currentChats.forEach { chat ->
                        try {
                            val response = httpClient.get("$serverBaseUrl/api/chats/${chat.id}/messages") {
                                accept(ContentType.Application.Json)
                            }
                            if (response.status.isSuccess()) {
                                val chatMessages = response.body<List<ChatMessage>>()
                                newMessages[chat.id] = chatMessages
                            }
                        } catch (e: Exception) {
                            println("❌ Failed to fetch messages for chat ${chat.id}: ${e.message}")
                        }
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
    
    /**
     * Send a message to a specific chat.
     */
    suspend fun sendMessage(chatId: String, messageText: String) {
        try {
            val userId = _currentUserId.value ?: "Unknown User"
            val request = SendMessageRequest(userId, messageText)
            
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
            
            val response = httpClient.post("$serverBaseUrl/api/chats") {
                contentType(ContentType.Application.Json)
                setBody(request)
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
                currentRequests[joinRequest.groupId] = currentRequests[joinRequest.groupId]?.filter { it.id != joinRequest.id } ?: emptyList()
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
                currentRequests[joinRequest.groupId] = currentRequests[joinRequest.groupId]?.filter { it.id != joinRequest.id } ?: emptyList()
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
     * Stop the service and clean up resources.
     */
    fun stop() {
        chatsPollingJob?.cancel()
        messagesPollingJob?.cancel()
        coroutineScope.cancel()
        httpClient.close()
    }
}
