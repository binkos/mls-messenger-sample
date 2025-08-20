package com.messenger.sample.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.sample.desktop.ui.components.GroupSelector
import com.messenger.sample.desktop.ui.components.MessagesArea
import com.messenger.sample.desktop.ui.components.MessageInput

@Composable
fun MessengerApp() {
    var selectedGroupId by remember { mutableStateOf("") }
    
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
                    onGroupSelected = { selectedGroupId = it },
                    modifier = Modifier.weight(1f)
                )
                
                // Create new group button
                Button(
                    onClick = { /* TODO: Implement group creation */ }
                ) {
                    Text("Create Group")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Messages area
            MessagesArea(
                groupId = selectedGroupId,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Message input
            MessageInput(
                groupId = selectedGroupId,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
