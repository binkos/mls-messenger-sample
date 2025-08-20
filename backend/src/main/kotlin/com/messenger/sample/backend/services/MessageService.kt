package com.messenger.sample.backend.services

import com.messenger.sample.backend.storage.InMemoryStorage
import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group
import java.util.UUID

class MessageService {
    private val storage = InMemoryStorage()
    
    fun getMessages(groupId: String): List<Message> {
        return if (groupId.isNotEmpty()) {
            storage.getMessagesByGroup(groupId)
        } else {
            storage.getAllMessages()
        }
    }
    
    fun saveMessage(message: Message): Message {
        val messageWithId = message.copy(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis()
        )
        storage.saveMessage(messageWithId)
        return messageWithId
    }
    
    fun getGroups(): List<Group> {
        return storage.getAllGroups()
    }
    
    fun createGroup(group: Group): Group {
        val groupWithId = group.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        storage.saveGroup(groupWithId)
        return groupWithId
    }
}
