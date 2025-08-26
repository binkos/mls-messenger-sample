package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.sample.shared.models.ChatGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatList(
    chats: List<ChatGroup>,
    selectedChatId: String?,
    onChatSelected: (String) -> Unit,
    onCreateNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onCreateNewChat,
                modifier = Modifier.height(36.dp)
            ) {
                Text("+ New")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chat list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chats) { chat ->
                ChatItem(
                    chat = chat,
                    isSelected = chat.id == selectedChatId,
                    onChatSelected = onChatSelected,
                    timeFormat = timeFormat
                )
            }
        }
    }
}

@Composable
private fun ChatItem(
    chat: ChatGroup,
    isSelected: Boolean,
    onChatSelected: (String) -> Unit,
    timeFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChatSelected(chat.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Chat info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                chat.lastMessage?.let { lastMessage ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            
            // Right side info
            Column(
                horizontalAlignment = Alignment.End
            ) {
                chat.lastMessageTime?.let { lastMessageTime ->
                    Text(
                        text = timeFormat.format(Date(lastMessageTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Badge(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
