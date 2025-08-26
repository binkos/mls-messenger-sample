package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EnterUserIdDialog(
    onConfirmed: (String) -> Unit
) {
    var currentUserId by remember { mutableStateOf("") }

    EnterUserIdDialogUI(
        currentUserId = currentUserId,
        { currentUserId = it },
        onConfirmClicked = {
            if (currentUserId.isNotEmpty()) {
                onConfirmed(currentUserId)
            }
        }
    )
}

@Composable
fun EnterUserIdDialogUI(
    currentUserId: String,
    onValueChange: (String) -> Unit,
    onConfirmClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Enter User ID") },
        text = {
            Column {
                Text("Please enter a unique user ID to initialize your client:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentUserId,
                    onValueChange = onValueChange,
                    label = { Text("User ID") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmClicked,
                enabled = currentUserId.isNotEmpty()
            ) {
                Text("Initialize Client")
            }
        }
    )
}