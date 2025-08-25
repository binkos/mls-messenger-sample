package com.messenger.sample.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.sample.desktop.ui.components.*
import com.messenger.sample.desktop.ui.models.*

@Composable
fun MessengerUI(
    modifier: Modifier = Modifier
) {
    // Sample data for demonstration
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var showJoinRequestDialog by remember { mutableStateOf(false) }
    var currentJoinRequest by remember { mutableStateOf<JoinRequest?>(null) }
    
    val sampleChats = remember {
        listOf(
            ChatGroup(
                id = "chat1",
                name = "General Chat",
                lastMessage = "Hello everyone!",
                lastMessageTime = System.currentTimeMillis() - 300000,
                unreadCount = 2
            ),
            ChatGroup(
                id = "chat2",
                name = "Project Team",
                lastMessage = "Meeting at 3 PM",
                lastMessageTime = System.currentTimeMillis() - 600000,
                unreadCount = 0
            ),
            ChatGroup(
                id = "chat3",
                name = "Random",
                lastMessage = "How's it going?",
                lastMessageTime = System.currentTimeMillis() - 900000,
                unreadCount = 1
            )
        )
    }
    
    val sampleMessages = remember {
        listOf(
            ChatMessage(
                id = "msg1",
                userName = "Alice",
                message = "Hello everyone! How's the project going?",
                timestamp = System.currentTimeMillis() - 300000,
                isOwnMessage = false
            ),
            ChatMessage(
                id = "msg2",
                userName = "You",
                message = "Hi Alice! Everything is on track. We should have the first prototype ready by Friday.",
                timestamp = System.currentTimeMillis() - 240000,
                isOwnMessage = true
            ),
            ChatMessage(
                id = "msg3",
                userName = "Bob",
                message = "Great! I've finished the backend API. Ready for integration testing.",
                timestamp = System.currentTimeMillis() - 180000,
                isOwnMessage = false
            ),
            ChatMessage(
                id = "msg4",
                userName = "Alice",
                message = "Perfect! Let's schedule a demo for the team.",
                timestamp = System.currentTimeMillis() - 120000,
                isOwnMessage = false
            )
        )
    }
    
    val sampleJoinRequest = remember {
        JoinRequest(
            id = "req1",
            userName = "Charlie",
            keyPackage = "eyJraWQiOiJ0ZXN0IiwidHlwZSI6ImtleV9wYWNrYWdlIiwidmVyc2lvbiI6MSwicGF5bG9hZCI6InRlc3QifQ==",
            groupId = "chat1",
            timestamp = System.currentTimeMillis() - 60000
        )
    }
    
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header
        Text(
            text = "MLS Messenger",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left sidebar - Chat list
            Card(
                modifier = Modifier.width(300.dp)
            ) {
                ChatList(
                    chats = sampleChats,
                    selectedChatId = selectedChatId,
                    onChatSelected = { chatId ->
                        selectedChatId = chatId
                    },
                    onCreateNewChat = {
                        // TODO: Implement new chat creation
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Main content area
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (selectedChatId != null) {
                    // Chat header
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sampleChats.find { it.id == selectedChatId }?.name ?: "",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            // Join request button (for demo purposes)
                            Button(
                                onClick = {
                                    currentJoinRequest = sampleJoinRequest
                                    showJoinRequestDialog = true
                                }
                            ) {
                                Text("Show Join Request")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Messages area
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(sampleMessages) { message ->
                                MessageDisplay(
                                    message = message,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Message input
                    MessageInput(
                        onMessageSent = { message ->
                            // TODO: Implement message sending
                            println("Sending message: $message")
                        },
                        enabled = selectedChatId != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // No chat selected
                    Card(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a chat to start messaging",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Join request dialog
    if (showJoinRequestDialog && currentJoinRequest != null) {
        JoinRequestDialog(
            joinRequest = currentJoinRequest!!,
            onAccept = { request ->
                // TODO: Implement accept logic
                println("Accepted join request from ${request.userName}")
                showJoinRequestDialog = false
                currentJoinRequest = null
            },
            onDecline = { request ->
                // TODO: Implement decline logic
                println("Declined join request from ${request.userName}")
                showJoinRequestDialog = false
                currentJoinRequest = null
            },
            onDismiss = {
                showJoinRequestDialog = false
                currentJoinRequest = null
            }
        )
    }
}
