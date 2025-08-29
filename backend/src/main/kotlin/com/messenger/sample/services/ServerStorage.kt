package com.messenger.sample.services

import com.messenger.sample.shared.models.ChatGroup
import com.messenger.sample.shared.models.ChatGroupWithUserStatus
import com.messenger.sample.shared.models.ChatMembershipStatus
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.Event
import com.messenger.sample.shared.models.EventType
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
    private val userChatStatuses =
        ConcurrentHashMap<String, MutableList<UserChatStatus>>() // userId -> List<UserChatStatus>
    private val events = ConcurrentHashMap<String, MutableList<Event>>() // userId -> List<Event>
    private var nextEventId = 1L
    private val connectedUsers = ConcurrentHashMap<String, Boolean>() // userId -> isConnected

    suspend fun getAllChats(): List<ChatGroup> {
        return chats.values.toList()
    }

    suspend fun getChat(chatId: String): ChatGroup? {
        return chats[chatId]
    }

    suspend fun addMessage(chatId: String, message: ChatMessage) {
        val chatMessages = messages.getOrPut(chatId) { mutableListOf() }
        userChatStatuses.forEach {
            val userId = it.key
            val groups = it.value

            val isGroupMember = groups.any { group -> group.chatId == chatId }
            if (it.key == message.userName && isGroupMember) {
                createEvent(
                    userId = userId,
                    type = EventType.MESSAGE_SENT,
                    chatId = chatId,
                    data = mapOf(
                        "id" to message.id,
                        "userName" to message.userName,
                        "message" to message.message,
                        "timestamp" to message.timestamp.toString()
                    )
                )
            }
        }
        chatMessages.add(message)
    }

    suspend fun getJoinRequests(chatId: String): List<JoinRequest> {
        return joinRequests[chatId]?.toList() ?: emptyList()
    }

    suspend fun addJoinRequest(joinRequest: JoinRequest) {
        val requests = joinRequests.getOrPut(joinRequest.groupId) { mutableListOf() }
        requests.add(joinRequest)

        // Create event for join request (notify group members)
        // For now, we'll create an event for the group creator
        // In a real app, you'd notify all group members
        val groupMembers = userChatStatuses.entries
            .filter { (_, statuses) -> statuses.any { it.chatId == joinRequest.groupId && it.status == ChatMembershipStatus.MEMBER } }
            .map { it.key }

        groupMembers.forEach { memberId ->
            createEvent(
                userId = memberId,
                type = EventType.JOIN_REQUESTED,
                chatId = joinRequest.groupId,
                data = mapOf(
                    "requesterName" to joinRequest.userName,
                    "requestId" to joinRequest.id,
                    "keyPackage" to joinRequest.keyPackage
                ),
//                userType = UserEventType.EXISTING_USER
            )
        }
    }

    suspend fun removeJoinRequest(chatId: String, requestId: String) {
        joinRequests[chatId]?.removeAll { it.id == requestId }
    }

    suspend fun createChat(name: String, creatorUserId: String? = null): ChatGroup = mutex.withLock {
        val chatId = "chat_${System.currentTimeMillis()}"
        val newChat = ChatGroup(
            id = chatId,
            name = name,
        )
        chats[chatId] = newChat
        messages[chatId] = mutableListOf()
        joinRequests[chatId] = mutableListOf()

        // Mark the creator as a member of the chat
        if (creatorUserId != null) {
            setUserChatStatus(creatorUserId, chatId, ChatMembershipStatus.MEMBER)

            // Create event for group creation (creator receives as CREATOR)
            createEvent(
                userId = creatorUserId,
                type = EventType.GROUP_CREATED,
                chatId = chatId,
                data = mapOf(
                    "name" to name,
                    "type" to ChatMembershipStatus.MEMBER.ordinal.toString()
                ),
            )

            // Send event to all existing users (they receive as EXISTING_USER)
            val existingUsers = userChatStatuses.keys.filter { it != creatorUserId }
            existingUsers.forEach { existingUserId ->
                createEvent(
                    userId = existingUserId,
                    type = EventType.GROUP_CREATED,
                    chatId = chatId,
                    data = mapOf(
                        "name" to name,
                        "type" to ChatMembershipStatus.NOT_MEMBER.ordinal.toString()
                    ),
                )
            }
        }

        newChat
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
        println("🔧 setUserChatStatus called: userId=$userId, chatId=$chatId, status=$status")

        val userStatuses = userChatStatuses.getOrPut(userId) { mutableListOf() }
        val existingStatus = userStatuses.find { it.chatId == chatId }

        if (existingStatus != null) {
            // Update existing status
            val index = userStatuses.indexOf(existingStatus)
            userStatuses[index] = existingStatus.copy(
                status = status,
                joinedAt = if (status == ChatMembershipStatus.MEMBER) System.currentTimeMillis() else existingStatus.joinedAt
            )
            println("✅ Updated existing status: $existingStatus -> ${userStatuses[index]}")
        } else {
            // Add new status
            val newStatus = UserChatStatus(
                userId = userId,
                chatId = chatId,
                status = status,
                joinedAt = if (status == ChatMembershipStatus.MEMBER) System.currentTimeMillis() else null
            )
            userStatuses.add(newStatus)
            println("✅ Added new status: $newStatus")
        }

        // Debug: print current statuses for this user
        println("🔍 Current statuses for user $userId: ${userStatuses}")
    }

    /**
     * Get chats with user status for a specific user
     */
    suspend fun getChatsWithUserStatus(userId: String): List<ChatGroupWithUserStatus> {
        val allChats = chats.values.toList()
        val userStatuses = userChatStatuses[userId] ?: emptyList()

        return allChats.map { chat ->
            val userStatus = userStatuses.find { it.chatId == chat.id }?.status ?: ChatMembershipStatus.NOT_MEMBER
            ChatGroupWithUserStatus(
                id = chat.id,
                name = chat.name,
                membershipStatus = userStatus
            )
        }
    }

    /**
     * Add an event for a specific user
     */
    private suspend fun addEvent(userId: String, event: Event) {
        val userEvents = events.getOrPut(userId) { mutableListOf() }
        userEvents.add(event)
    }

    /**
     * Get events for a user since a specific event ID
     */
    suspend fun getUserEvents(userId: String, sinceEventId: String? = null): List<Event> {
        val userEvents = events[userId] ?: emptyList()
        if (sinceEventId == null) {
            return userEvents
        }

        val sinceIndex = userEvents.indexOfFirst { it.id == sinceEventId }
        return if (sinceIndex == -1) {
            userEvents
        } else {
            userEvents.subList(sinceIndex + 1, userEvents.size)
        }
    }

    /**
     * Create and add an event
     */
    suspend fun createEvent(
        userId: String,
        type: EventType,
        chatId: String? = null,
        data: Map<String, String> = emptyMap(),
    ): Event {
        val eventId = "evt_${nextEventId++}"
        val event = Event(
            id = eventId,
            type = type,
            userId = userId,
            chatId = chatId,
            data = data,
            timestamp = System.currentTimeMillis(),
        )
        addEvent(userId, event)
        return event
    }

    /**
     * Send existing groups to a new user (when they connect)
     */
    suspend fun sendExistingGroupsToUser(userId: String) {
        val allChats = chats.values.toList()
        allChats.forEach { chat ->
            createEvent(
                userId = userId,
                type = EventType.GROUP_CREATED,
                chatId = chat.id,
                data = mapOf(
                    "name" to chat.name
                ),
            )
        }
    }
}
