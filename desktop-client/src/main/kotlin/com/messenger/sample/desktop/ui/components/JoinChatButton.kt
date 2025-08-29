package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.sample.shared.models.ChatGroupWithUserStatus
import com.messenger.sample.shared.models.ChatMembershipStatus

@Composable
fun JoinChatButton(
    chat: ChatGroupWithUserStatus,
    onRequestToJoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (chat.membershipStatus) {
        ChatMembershipStatus.NOT_MEMBER -> {
            Card(
                modifier = modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You are not a member of this chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onRequestToJoin(chat.id) }
                    ) {
                        Text("Request to Join")
                    }
                }
            }
        }
        
        ChatMembershipStatus.PENDING -> {
            Card(
                modifier = modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Join request pending...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Waiting for approval",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        ChatMembershipStatus.MEMBER -> {
            // User is already a member, no button needed
        }
    }
}
