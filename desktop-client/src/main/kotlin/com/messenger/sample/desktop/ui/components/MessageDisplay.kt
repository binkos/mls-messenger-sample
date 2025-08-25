package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.messenger.sample.desktop.ui.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageDisplay(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val messageTime = remember { timeFormat.format(Date(message.timestamp)) }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // User name
            Text(
                text = message.userName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            // Message bubble
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = if (message.isOwnMessage) 16.dp else 4.dp,
                            topEnd = if (message.isOwnMessage) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(
                        if (message.isOwnMessage) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isOwnMessage) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Timestamp
            Text(
                text = messageTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
