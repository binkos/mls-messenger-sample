package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageInput(
    groupId: String,
    onMessageSent: (String) -> Unit,
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
            enabled = groupId.isNotEmpty(),
            placeholder = { 
                if (groupId.isEmpty()) {
                    Text("Select a group first")
                } else {
                    Text("Type your message")
                }
            }
        )
        
        Button(
            onClick = { 
                onMessageSent(messageText)
                messageText = ""
            },
            enabled = groupId.isNotEmpty() && messageText.isNotEmpty()
        ) {
            Text("Send")
        }
    }
}
