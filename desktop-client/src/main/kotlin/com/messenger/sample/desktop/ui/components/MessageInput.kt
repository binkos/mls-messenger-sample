package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    onMessageSent: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Type your message") },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            placeholder = { 
                if (!enabled) {
                    Text("Select a chat first")
                } else {
                    Text("Type your message")
                }
            },
            singleLine = false,
            minLines = 1,
            maxLines = 4
        )
        
        Button(
            onClick = { 
                if (messageText.isNotEmpty()) {
                    onMessageSent(messageText)
                    messageText = ""
                }
            },
            enabled = enabled && messageText.isNotEmpty()
        ) {
            Text("Send")
        }
    }
}
