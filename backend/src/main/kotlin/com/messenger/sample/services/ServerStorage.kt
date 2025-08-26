package com.messenger.sample.services

import com.messenger.sample.shared.models.ChatGroup
import com.messenger.sample.shared.models.ChatMembershipStatus
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.JoinRequest
import com.messenger.sample.shared.models.UserChatStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


/**
 * Simple in-memory server storage for chats and messages.
 * In a real application, this would be replaced with a proper database.
 */
object ServerStorage {
    private val mutex = Mutex()
    private val chats = ConcurrentHashMap<String, ChatGroup>()
    private val messages = ConcurrentHashMap<String, MutableList<ChatMessage>>()
    private val joinRequests = ConcurrentHashMap<String, MutableList<JoinRequest>>()
    private val userChatStatuses = ConcurrentHashMap<String, MutableList<UserChatStatus>>() // userId -> List<UserChatStatus>
    
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
    
    suspend fun createChat(name: String, creatorUserId: String? = null): ChatGroup = mutex.withLock {
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
        
        // Mark the creator as a member of the chat
        if (creatorUserId != null) {
            setUserChatStatus(creatorUserId, chatId, ChatMembershipStatus.MEMBER)
        }
        
        newChat
    }
    
    suspend fun markChatAsRead(chatId: String) = mutex.withLock {
        chats[chatId]?.let { chat ->
            chats[chatId] = chat.copy(unreadCount = 0)
        }
    }
    
    /**
     * Get user status for all chats
     */
    suspend fun getUserChatStatuses(userId: String): List<UserChatStatus> = mutex.withLock {
        userChatStatuses[userId]?.toList() ?: emptyList()
    }
    
    /**
     * Get user status for a specific chat
     */
    suspend fun getUserChatStatus(userId: String, chatId: String): ChatMembershipStatus = mutex.withLock {
        val userStatuses = userChatStatuses[userId] ?: emptyList()
        val status = userStatuses.find { it.chatId == chatId }
        status?.status ?: ChatMembershipStatus.NOT_MEMBER
    }
    
    /**
     * Add or update user status for a chat
     */
    suspend fun setUserChatStatus(userId: String, chatId: String, status: ChatMembershipStatus) {
        val userStatuses = userChatStatuses.getOrPut(userId) { mutableListOf() }
        val existingStatus = userStatuses.find { it.chatId == chatId }
        
        if (existingStatus != null) {
            // Update existing status
            val index = userStatuses.indexOf(existingStatus)
            userStatuses[index] = existingStatus.copy(
                status = status,
                joinedAt = if (status == ChatMembershipStatus.MEMBER) System.currentTimeMillis() else existingStatus.joinedAt
            )
        } else {
            // Add new status
            userStatuses.add(UserChatStatus(
                userId = userId,
                chatId = chatId,
                status = status,
                joinedAt = if (status == ChatMembershipStatus.MEMBER) System.currentTimeMillis() else null
            ))
        }
    }
    
    /**
     * Get chats with user status for a specific user
     */
    suspend fun getChatsWithUserStatus(userId: String): List<com.messenger.sample.shared.models.ChatGroupWithUserStatus> = mutex.withLock {
        val allChats = chats.values.toList()
        val userStatuses = userChatStatuses[userId] ?: emptyList()
        
        allChats.map { chat ->
            val userStatus = userStatuses.find { it.chatId == chat.id }?.status ?: ChatMembershipStatus.NOT_MEMBER
            com.messenger.sample.shared.models.ChatGroupWithUserStatus(
                id = chat.id,
                name = chat.name,
                lastMessage = chat.lastMessage,
                lastMessageTime = chat.lastMessageTime,
                unreadCount = chat.unreadCount,
                userStatus = userStatus
            )
        }
    }
}
