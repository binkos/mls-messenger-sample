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
    
    suspend fun getJoinRequests(chatId: String): List<JoinRequest> {
        return joinRequests[chatId]?.toList() ?: emptyList()
    }
    
    suspend fun addJoinRequest(joinRequest: JoinRequest) {
        val requests = joinRequests.getOrPut(joinRequest.groupId) { mutableListOf() }
        requests.add(joinRequest)
    }
    
    suspend fun removeJoinRequest(chatId: String, requestId: String) {
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
        joinRequests[chatId] = mutableListOf()
        
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
    suspend fun getUserChatStatuses(userId: String): List<UserChatStatus> {
        return userChatStatuses[userId]?.toList() ?: emptyList()
    }
    
    /**
     * Get user status for a specific chat
     */
    suspend fun getUserChatStatus(userId: String, chatId: String): ChatMembershipStatus {
        val userStatuses = userChatStatuses[userId] ?: emptyList()
        val status = userStatuses.find { it.chatId == chatId }
        return status?.status ?: ChatMembershipStatus.NOT_MEMBER
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
    suspend fun getChatsWithUserStatus(userId: String): List<com.messenger.sample.shared.models.ChatGroupWithUserStatus> {
        val allChats = chats.values.toList()
        val userStatuses = userChatStatuses[userId] ?: emptyList()
        
        return allChats.map { chat ->
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
