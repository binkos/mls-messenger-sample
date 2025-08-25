package com.messenger.sample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import uniffi.mls_rs_uniffi.CipherSuite
import uniffi.mls_rs_uniffi.generateSignatureKeypair
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication, title = "MLS Messenger Client", state = rememberWindowState()
    ) {
        try {
            // Now try to use the UniFFI functions
            val keypair = generateSignatureKeypair(CipherSuite.CURVE25519_AES128)
            println("Successfully generated keypair")
        } catch (e: Exception) {
            println("Failed to load native library: ${e.message}")
            e.printStackTrace()
        }
    }
}