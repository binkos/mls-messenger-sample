package com.messenger.sample.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val groupId: String,
    val encryptedContent: String? = null
)

@Serializable
data class Group(
    val id: String,
    val name: String,
    val memberIds: List<String>,
    val createdAt: Long
)

@Serializable
data class User(
    val id: String,
    val name: String,
    val publicKey: String
)
