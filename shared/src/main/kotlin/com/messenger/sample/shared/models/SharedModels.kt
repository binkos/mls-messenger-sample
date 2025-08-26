package com.messenger.sample.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val userName: String,
    val message: String,
    val timestamp: Long,
    val isOwnMessage: Boolean = false
)

@Serializable
data class ChatGroup(
    val id: String,
    val name: String,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
)

@Serializable
data class JoinRequest(
    val id: String,
    val userName: String,
    val keyPackage: String,
    val groupId: String,
    val timestamp: Long
)

@Serializable
data class CreateChatRequest(val name: String)

@Serializable
data class SendMessageRequest(val userName: String, val message: String)

@Serializable
data class CreateJoinRequestRequest(
    val userName: String,
    val keyPackage: String,
    val groupId: String
)
