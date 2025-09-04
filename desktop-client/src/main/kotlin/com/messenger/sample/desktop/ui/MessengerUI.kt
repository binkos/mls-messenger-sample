package com.messenger.sample.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.sample.desktop.services.HttpClientService
import com.messenger.sample.desktop.ui.components.ChatList
import com.messenger.sample.desktop.ui.components.EnterUserIdDialog
import com.messenger.sample.desktop.ui.components.JoinChatButton
import com.messenger.sample.desktop.ui.components.JoinRequestDialog
import com.messenger.sample.desktop.ui.components.MessageDisplay
import com.messenger.sample.desktop.ui.components.MessageInput
import com.messenger.sample.shared.models.ChatGroupWithUserStatus
import com.messenger.sample.shared.models.ChatMembershipStatus
import com.messenger.sample.shared.models.JoinRequest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MessengerUI(
    modifier: Modifier = Modifier
) {
    val clientService = remember { HttpClientService() }
    val coroutineScope = rememberCoroutineScope()

    val selectedChatId by clientService.selectedChatIdFlow.collectAsState()
    var showJoinRequestDialog by remember { mutableStateOf(false) }
    var currentJoinRequest by remember { mutableStateOf<JoinRequest?>(null) }
    var showUserIdDialog by remember { mutableStateOf(true) }
    val currentUser by clientService.currentDesktopUserId.collectAsState()

    val chats by clientService.chats.collectAsState()
    val selectedChatMessages by clientService.messagesFlow.collectAsState(emptyList())

    val joinRequests by clientService.joinRequests.collectAsState()

    // User ID input dialog
    if (showUserIdDialog) {
        EnterUserIdDialog(
            onConfirmed = { userName ->
                coroutineScope.launch {
                    try {
                        clientService.initialize(userName)
                        showUserIdDialog = false
                    } catch (e: Exception) {
                        println("Error creating user: ${e.message}")
                    }
                }
            }
        )
    }

    // Join request dialog
    if (showJoinRequestDialog && currentJoinRequest != null) {
        val groupName = chats.find { it.id == currentJoinRequest!!.groupId }?.name
        JoinRequestDialog(
            joinRequest = currentJoinRequest!!,
            groupName = groupName,
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
                    if (currentUser != null) {
                        Text(
                            text = "User: ${currentUser?.userName}",
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
                            clientService.selectedChatIdFlow.value = chatId
                        },
                        onCreateNewChat = {
                            coroutineScope.launch {
                                try {
                                    val newChat =
                                        clientService.createChat("New Chat ${System.currentTimeMillis() % 1000}")
                                    clientService.selectedChatIdFlow.value = newChat.id
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
                        val selectedChat = chats.find { it.id == selectedChatId }

                        if (selectedChat != null) {
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
                                        text = selectedChat.name,
                                        style = MaterialTheme.typography.titleLarge
                                    )

                                    // Join requests button (only for members)
                                    if (selectedChat.membershipStatus == ChatMembershipStatus.MEMBER) { // For now, assume all chats are member chats
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
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Show join button if user is not a member
                            if (selectedChat.membershipStatus != ChatMembershipStatus.MEMBER) { // For now, assume all users are members
                                JoinChatButton(
                                    chat = ChatGroupWithUserStatus(
                                        id = selectedChat.id,
                                        name = selectedChat.name,
                                        membershipStatus = selectedChat.membershipStatus
                                    ),
                                    onRequestToJoin = { chatId ->
                                        coroutineScope.launch {
                                            try {
                                                val success = clientService.requestToJoinChat(chatId)
                                                if (success) {
                                                    println("✅ Join request sent successfully")
                                                } else {
                                                    println("❌ Failed to send join request")
                                                }
                                            } catch (e: Exception) {
                                                println("Error sending join request: ${e.message}")
                                            }
                                        }
                                    }
                                )
                            } else {
                                // User is a member, show messages
                                // Messages area
                                Card(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (selectedChatMessages.isNotEmpty()) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(
                                                items = selectedChatMessages,
                                                key = { it.id }
                                            ) { message ->
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
                            }
                        }
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
