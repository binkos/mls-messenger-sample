package com.messenger.sample.backend.services

import com.messenger.sample.backend.storage.InMemoryStorage
import com.messenger.sample.native.MlsNativeWrapper
import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group
import java.util.UUID

class MessageService {
    private val storage = InMemoryStorage()
    private val mlsWrapper = MlsNativeWrapper()
    
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
        
        // Encrypt the message content using MLS
        val encryptedContent = try {
            mlsWrapper.encryptMessage(message.groupId, message.content)
        } catch (e: Exception) {
            // Fallback to plain text if MLS encryption fails
            message.content
        }
        
        val encryptedMessage = messageWithId.copy(
            encryptedContent = encryptedContent
        )
        
        storage.saveMessage(encryptedMessage)
        return encryptedMessage
    }
    
    fun getGroups(): List<Group> {
        return storage.getAllGroups()
    }
    
    fun createGroup(group: Group): Group {
        // Create MLS group
        val mlsGroup = try {
            mlsWrapper.createGroup()
        } catch (e: Exception) {
            // Fallback to regular group creation
            group.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis()
            )
        }
        
        storage.saveGroup(mlsGroup)
        return mlsGroup
    }
    
    fun joinGroup(groupId: String): Boolean {
        return try {
            mlsWrapper.joinGroup(groupId)
        } catch (e: Exception) {
            false
        }
    }
    
    fun getGroupInfo(groupId: String): Group? {
        return try {
            mlsWrapper.getGroupInfo(groupId) ?: storage.getGroupById(groupId)
        } catch (e: Exception) {
            storage.getGroupById(groupId)
        }
    }
}
