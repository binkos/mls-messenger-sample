package com.messenger.sample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.messenger.sample.desktop.ui.MessengerApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "MLS Messenger Client",
        state = rememberWindowState()
    ) {
        MessengerApp()
    }
}
