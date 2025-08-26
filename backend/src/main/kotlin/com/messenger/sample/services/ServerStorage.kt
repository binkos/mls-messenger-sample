package com.messenger.sample.services

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Data models for the server storage
 */
data class ChatMessage(
    val id: String,
    val userName: String,
    val message: String,
    val timestamp: Long,
    val isOwnMessage: Boolean = false
)

data class ChatGroup(
    val id: String,
    val name: String,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
)

data class JoinRequest(
    val id: String,
    val userName: String,
    val keyPackage: String,
    val groupId: String,
    val timestamp: Long
)

/**
 * Simple in-memory server storage for chats and messages.
 * In a real application, this would be replaced with a proper database.
 */
object ServerStorage {
    private val mutex = Mutex()
    private val chats = ConcurrentHashMap<String, ChatGroup>()
    private val messages = ConcurrentHashMap<String, MutableList<ChatMessage>>()
    private val joinRequests = ConcurrentHashMap<String, MutableList<JoinRequest>>()
    
    init {
        // Initialize with sample data
        setupSampleData()
    }
    
    private fun setupSampleData() {
        // Sample chats
        chats["chat1"] = ChatGroup(
            id = "chat1",
            name = "General Chat",
            lastMessage = "Hello everyone!",
            lastMessageTime = System.currentTimeMillis() - 300000,
            unreadCount = 2
        )
        
        chats["chat2"] = ChatGroup(
            id = "chat2",
            name = "Project Team",
            lastMessage = "Meeting at 3 PM",
            lastMessageTime = System.currentTimeMillis() - 600000,
            unreadCount = 0
        )
        
        chats["chat3"] = ChatGroup(
            id = "chat3",
            name = "Random",
            lastMessage = "How's it going?",
            lastMessageTime = System.currentTimeMillis() - 900000,
            unreadCount = 1
        )
        
        // Sample messages for chat1
        messages["chat1"] = mutableListOf(
            ChatMessage(
                id = "msg1",
                userName = "Alice",
                message = "Hello everyone! How's the project going?",
                timestamp = System.currentTimeMillis() - 300000,
                isOwnMessage = false
            ),
            ChatMessage(
                id = "msg2",
                userName = "You",
                message = "Hi Alice! Everything is on track. We should have the first prototype ready by Friday.",
                timestamp = System.currentTimeMillis() - 240000,
                isOwnMessage = true
            ),
            ChatMessage(
                id = "msg3",
                userName = "Bob",
                message = "Great! I've finished the backend API. Ready for integration testing.",
                timestamp = System.currentTimeMillis() - 180000,
                isOwnMessage = false
            ),
            ChatMessage(
                id = "msg4",
                userName = "Alice",
                message = "Perfect! Let's schedule a demo for the team.",
                timestamp = System.currentTimeMillis() - 120000,
                isOwnMessage = false
            )
        )
        
        // Sample messages for chat2
        messages["chat2"] = mutableListOf(
            ChatMessage(
                id = "msg5",
                userName = "Bob",
                message = "Team meeting scheduled for 3 PM today.",
                timestamp = System.currentTimeMillis() - 600000,
                isOwnMessage = false
            ),
            ChatMessage(
                id = "msg6",
                userName = "You",
                message = "I'll prepare the presentation slides.",
                timestamp = System.currentTimeMillis() - 540000,
                isOwnMessage = true
            )
        )
        
        // Sample messages for chat3
        messages["chat3"] = mutableListOf(
            ChatMessage(
                id = "msg7",
                userName = "Charlie",
                message = "How's it going everyone?",
                timestamp = System.currentTimeMillis() - 900000,
                isOwnMessage = false
            )
        )
        
        // Sample join request
        joinRequests["chat1"] = mutableListOf(
            JoinRequest(
                id = "req1",
                userName = "Charlie",
                keyPackage = "eyJraWQiOiJ0ZXN0IiwidHlwZSI6ImtleV9wYWNrYWdlIiwidmVyc2lvbiI6MSwicGF5bG9hZCI6InRlc3QifQ==",
                groupId = "chat1",
                timestamp = System.currentTimeMillis() - 60000
            )
        )
    }
    
    suspend fun getAllChats(): List<ChatGroup> = mutex.withLock {
        chats.values.toList()
    }
    
    suspend fun getChat(chatId: String): ChatGroup? = mutex.withLock {
        chats[chatId]
    }
    
    suspend fun getMessages(chatId: String): List<ChatMessage> = mutex.withLock {
        messages[chatId]?.toList() ?: emptyList()
    }
    
    suspend fun addMessage(chatId: String, message: ChatMessage) = mutex.withLock {
        val chatMessages = messages.getOrPut(chatId) { mutableListOf() }
        chatMessages.add(message)
        
        // Update chat's last message
        chats[chatId]?.let { chat ->
            chats[chatId] = chat.copy(
                lastMessage = message.message,
                lastMessageTime = message.timestamp,
                unreadCount = chat.unreadCount + 1
            )
        }
    }
    
    suspend fun getJoinRequests(chatId: String): List<JoinRequest> = mutex.withLock {
        joinRequests[chatId]?.toList() ?: emptyList()
    }
    
    suspend fun addJoinRequest(joinRequest: JoinRequest) = mutex.withLock {
        val requests = joinRequests.getOrPut(joinRequest.groupId) { mutableListOf() }
        requests.add(joinRequest)
    }
    
    suspend fun removeJoinRequest(chatId: String, requestId: String) = mutex.withLock {
        joinRequests[chatId]?.removeAll { it.id == requestId }
    }
    
    suspend fun createChat(name: String): ChatGroup = mutex.withLock {
        val chatId = "chat_${System.currentTimeMillis()}"
        val newChat = ChatGroup(
            id = chatId,
            name = name,
            lastMessage = null,
            lastMessageTime = null,
            unreadCount = 0
        )
        chats[chatId] = newChat
        messages[chatId] = mutableListOf()
        newChat
    }
    
    suspend fun markChatAsRead(chatId: String) = mutex.withLock {
        chats[chatId]?.let { chat ->
            chats[chatId] = chat.copy(unreadCount = 0)
        }
    }
}
