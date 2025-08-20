package com.messenger.sample.backend.storage

import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group

class InMemoryStorage {
    private val messages = mutableListOf<Message>()
    private val groups = mutableListOf<Group>()
    
    fun saveMessage(message: Message) {
        messages.add(message)
    }
    
    fun getAllMessages(): List<Message> {
        return messages.toList()
    }
    
    fun getMessagesByGroup(groupId: String): List<Message> {
        return messages.filter { it.groupId == groupId }
    }
    
    fun saveGroup(group: Group) {
        groups.add(group)
    }
    
    fun getAllGroups(): List<Group> {
        return groups.toList()
    }
    
    fun getGroupById(groupId: String): Group? {
        return groups.find { it.id == groupId }
    }
}
