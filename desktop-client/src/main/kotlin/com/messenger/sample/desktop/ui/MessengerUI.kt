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
import com.messenger.sample.desktop.services.HttpClientService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun MessengerUI(
    modifier: Modifier = Modifier
) {
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    var showJoinRequestDialog by remember { mutableStateOf(false) }
    var currentJoinRequest by remember { mutableStateOf<JoinRequest?>(null) }
    var showUserIdDialog by remember { mutableStateOf(true) }
    var currentUserId by remember { mutableStateOf("") }
    
    val clientService = remember { HttpClientService() }
    val coroutineScope = rememberCoroutineScope()
    
    // Collect data from ClientService
    val chats by clientService.chats.collectAsState()
    val messages by clientService.messages.collectAsState()
    val joinRequests by clientService.joinRequests.collectAsState()
    
    // Mark chat as read when selected
    LaunchedEffect(selectedChatId) {
        selectedChatId?.let { chatId ->
            delay(1000) // Small delay to ensure UI is updated
            clientService.markChatAsRead(chatId)
        }
    }
    
    // User ID input dialog
    if (showUserIdDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Enter User ID") },
            text = {
                Column {
                    Text("Please enter a unique user ID to initialize your client:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = currentUserId,
                        onValueChange = { currentUserId = it },
                        label = { Text("User ID") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        if (currentUserId.isNotEmpty()) {
                            clientService.initialize(currentUserId)
                            showUserIdDialog = false
                        }
                    },
                    enabled = currentUserId.isNotEmpty()
                ) {
                    Text("Initialize Client")
                }
            }
        )
    }
    
    // Join request dialog
    if (showJoinRequestDialog && currentJoinRequest != null) {
        JoinRequestDialog(
            joinRequest = currentJoinRequest!!,
            onAccept = { request ->
                coroutineScope.launch {
                    try {
                        clientService.acceptJoinRequest(request)
                        showJoinRequestDialog = false
                        currentJoinRequest = null
                    } catch (e: Exception) {
                        println("Error accepting join request: ${e.message}")
                    }
                }
            },
            onDecline = { request ->
                coroutineScope.launch {
                    try {
                        clientService.declineJoinRequest(request)
                        showJoinRequestDialog = false
                        currentJoinRequest = null
                    } catch (e: Exception) {
                        println("Error declining join request: ${e.message}")
                    }
                }
            },
            onDismiss = {
                showJoinRequestDialog = false
                currentJoinRequest = null
            }
        )
    }

    MaterialTheme {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MLS Messenger",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (currentUserId.isNotEmpty()) {
                        Text(
                            text = "User: $currentUserId",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left sidebar - Chat list
                Card(
                    modifier = Modifier.width(300.dp)
                ) {
                    ChatList(
                        chats = chats,
                        selectedChatId = selectedChatId,
                        onChatSelected = { chatId ->
                            selectedChatId = chatId
                        },
                        onCreateNewChat = {
                            coroutineScope.launch {
                                try {
                                    val newChat = clientService.createChat("New Chat ${System.currentTimeMillis() % 1000}")
                                    selectedChatId = newChat.id
                                } catch (e: Exception) {
                                    println("Error creating chat: ${e.message}")
                                }
                            }
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
                                    text = chats.find { it.id == selectedChatId }?.name ?: "",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                
                                // Join requests button
                                val chatJoinRequests = joinRequests[selectedChatId] ?: emptyList()
                                if (chatJoinRequests.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            currentJoinRequest = chatJoinRequests.first()
                                            showJoinRequestDialog = true
                                        }
                                    ) {
                                        Text("Join Requests (${chatJoinRequests.size})")
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Messages area
                        Card(
                            modifier = Modifier.weight(1f)
                        ) {
                            val chatMessages = messages[selectedChatId] ?: emptyList()
                            if (chatMessages.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(chatMessages) { message ->
                                        MessageDisplay(
                                            message = message,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No messages yet. Start the conversation!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Message input
                        MessageInput(
                            onMessageSent = { messageText ->
                                if (selectedChatId != null && messageText.isNotEmpty()) {
                                    coroutineScope.launch {
                                        try {
                                            clientService.sendMessage(selectedChatId!!, messageText)
                                        } catch (e: Exception) {
                                            println("Error sending message: ${e.message}")
                                        }
                                    }
                                }
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
    }
    
    // Cleanup when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            clientService.stop()
        }
    }
}
