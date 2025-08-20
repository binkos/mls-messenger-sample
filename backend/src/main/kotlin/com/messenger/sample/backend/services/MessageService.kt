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
        
        // Store the message as-is - encryption should happen on client side
        // Server should never see plain text content
        storage.saveMessage(messageWithId)
        return messageWithId
    }
    
    fun getGroups(): List<Group> {
        return storage.getAllGroups()
    }
    
    fun createGroup(group: Group): Group {
        // Create regular group - MLS operations happen on client side
        val newGroup = group.copy(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        
        storage.saveGroup(newGroup)
        return newGroup
    }
    
    fun joinGroup(groupId: String): Boolean {
        // Simple group joining - no MLS involved
        // groupId parameter kept for API consistency
        return true
    }
    
    fun getGroupInfo(groupId: String): Group? {
        return storage.getGroupById(groupId)
    }
}
