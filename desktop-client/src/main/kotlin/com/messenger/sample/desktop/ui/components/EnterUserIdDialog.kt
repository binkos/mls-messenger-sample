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
    var currentUserName by remember { mutableStateOf("") }

    EnterUserIdDialogUI(
        currentUserName = currentUserName,
        { currentUserName = it },
        onConfirmClicked = {
            if (currentUserName.isNotEmpty()) {
                onConfirmed(currentUserName)
            }
        }
    )
}

@Composable
fun EnterUserIdDialogUI(
    currentUserName: String,
    onValueChange: (String) -> Unit,
    onConfirmClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Enter User Name") },
        text = {
            Column {
                Text("Please enter your name to create a new user account:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentUserName,
                    onValueChange = onValueChange,
                    label = { Text("User Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmClicked,
                enabled = currentUserName.isNotEmpty()
            ) {
                Text("Create User")
            }
        }
    )
}