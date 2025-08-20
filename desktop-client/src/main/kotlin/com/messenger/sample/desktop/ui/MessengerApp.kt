package com.messenger.sample.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberCoroutineScope
import com.messenger.sample.desktop.ui.components.GroupSelector
import com.messenger.sample.desktop.ui.components.MessagesArea
import com.messenger.sample.desktop.ui.components.MessageInput
import com.messenger.sample.desktop.services.BackendService
import com.messenger.sample.shared.models.Group
import com.messenger.sample.shared.models.Message

@Composable
fun MessengerApp() {
    var selectedGroupId by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }

    
    val backendService = remember { BackendService() }
    val coroutineScope = rememberCoroutineScope()
    
    // Load groups on startup and refresh periodically
    LaunchedEffect(Unit) {
        while (true) {
            try {
                groups = backendService.getGroups()
            } catch (e: Exception) {
                // Handle error - could show error state
                println("Error loading groups: ${e.message}")
            }
            delay(5000) // Refresh groups every 5 seconds
        }
    }
    
    // Load messages when group changes and poll for new messages
    LaunchedEffect(selectedGroupId) {
        if (selectedGroupId.isNotEmpty()) {
            while (true) {
                try {
                    messages = backendService.getMessages(selectedGroupId)
                } catch (e: Exception) {
                    // Handle error
                    println("Error loading messages: ${e.message}")
                }
                delay(1500) // Poll every 1.5 seconds
            }
        } else {
            // Clear messages if no group selected
            messages = emptyList()
        }
    }
    
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = "MLS Messenger Client",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group selection
                GroupSelector(
                    selectedGroupId = selectedGroupId,
                    groups = groups,
                    onGroupSelected = { selectedGroupId = it },
                    modifier = Modifier.weight(1f)
                )
                
                // Create new group button
                Button(
                    onClick = { 
                        coroutineScope.launch {
                            try {
                                val newGroup = backendService.createGroup(Group(
                                    id = "",
                                    name = "New Group ${System.currentTimeMillis() % 1000}",
                                    memberIds = emptyList(),
                                    createdAt = 0
                                ))
                                // Group will be picked up by the polling mechanism
                                selectedGroupId = newGroup.id
                            } catch (e: Exception) {
                                println("Error creating group: ${e.message}")
                            }
                        }
                    }
                ) {
                    Text("Create Group")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Messages area
            MessagesArea(
                groupId = selectedGroupId,
                messages = messages,
                onDecryptMessage = { encryptedContent ->
                    backendService.decryptMessage(encryptedContent)
                },
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Message input
            MessageInput(
                groupId = selectedGroupId,
                onMessageSent = { messageText ->
                    if (selectedGroupId.isNotEmpty() && messageText.isNotEmpty()) {
                        try {
                            val message = Message(
                                id = "",
                                senderId = "user_${System.currentTimeMillis() % 1000}",
                                content = messageText, // Plain text - will be encrypted by BackendService
                                timestamp = System.currentTimeMillis(),
                                groupId = selectedGroupId
                            )
                            
                            // Launch coroutine for async operation
                            coroutineScope.launch {
                                try {
                                    backendService.sendMessage(message)
                                    // Message will be picked up by the polling mechanism
                                } catch (e: Exception) {
                                    println("Error sending message: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            // Handle error
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
