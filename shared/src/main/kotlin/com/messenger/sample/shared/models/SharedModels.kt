package com.messenger.sample.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val userId: String,
    val userName: String,
    val message: String,
    val timestamp: Long,
    val isOwnMessage: Boolean = false
)

@Serializable
data class ChatGroup(
    val id: String,
    val name: String,
)

@Serializable
data class JoinRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val keyPackage: String,
    val groupId: String,
    val timestamp: Long
)

@Serializable
data class CreateChatRequest(val name: String)

@Serializable
data class SendMessageRequest(
    val userId: String,
    val userName: String,
    val message: String
)

@Serializable
data class CreateJoinRequestRequest(
    val userId: String,
    val keyPackage: String,
    val groupId: String
)

@Serializable
data class CreateUserRequest(
    val userName: String
)

@Serializable
data class CreateUserResponse(
    val userId: String,
    val userName: String
)

@Serializable
data class AcceptJoinRequestRequest(
    val approverId: String,
    val chatId: String,
    val welcomeMessage: String,  // Base64 encoded welcome message
    val commitMessage: String// Base64 encoded commit message
)

@Serializable
data class UserChatStatus(
    val userId: String,
    val chatId: String,
    val status: ChatMembershipStatus,
    val joinedAt: Long? = null
)

@Serializable
enum class ChatMembershipStatus {
    NOT_MEMBER,     // User is not a member and hasn't requested to join
    MEMBER,        // User is a member of the chat
    PENDING,       // User has requested to join (waiting for approval)
}

@Serializable
data class ChatGroupWithUserStatus(
    val id: String,
    val name: String,
    val membershipStatus: ChatMembershipStatus = ChatMembershipStatus.NOT_MEMBER
)

@Serializable
data class Event(
    val id: String,
    val type: EventType,
    val userId: String, // Target user who should receive this event
    val chatId: String? = null,
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

@Serializable
enum class EventType {
    GROUP_CREATED,      // User created a group
    JOIN_REQUESTED,     // Someone requested to join a group
    JOIN_APPROVED,      // Join request was approved
    JOIN_DECLINED,      // Join request was declined
    JOIN_REQUEST_STATUS_UPDATE, // Join request status was updated (accepted/declined)
    USER_JOINED,        // User joined a group
    USER_LEFT,          // User left a group
    MESSAGE_SENT,       // Message sent
}
