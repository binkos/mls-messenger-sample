package com.messenger.sample.desktop.ui.models

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
