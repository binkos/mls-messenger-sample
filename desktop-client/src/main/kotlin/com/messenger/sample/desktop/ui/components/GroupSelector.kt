package com.messenger.sample.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupSelector(
    selectedGroupId: String,
    groups: List<com.messenger.sample.shared.models.Group>,
    onGroupSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
            ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = modifier
        ) {
            OutlinedTextField(
                value = groups.find { it.id == selectedGroupId }?.name ?: "Select a group",
                onValueChange = {},
                readOnly = true,
                label = { Text("Group") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            onGroupSelected(group.id)
                            expanded = false
                        }
                    )
                }
            }
        }
}
