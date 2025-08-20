package com.messenger.sample.native

import com.messenger.sample.shared.models.Group
import com.messenger.sample.shared.models.Message
import java.io.File

class MlsNativeWrapper {
    companion object {
        init {
            try {
                // Load the native library
                val libraryPath = System.getProperty("os.name").lowercase()
                val libraryName = when {
                    libraryPath.contains("mac") -> "libmls_native.dylib"
                    libraryPath.contains("linux") -> "libmls_native.so"
                    libraryPath.contains("windows") -> "mls_native.dll"
                    else -> throw RuntimeException("Unsupported operating system")
                }
                
                System.loadLibrary("mls_native")
            } catch (e: UnsatisfiedLinkError) {
                throw RuntimeException("Failed to load native MLS library", e)
            }
        }
    }
    
    // Native method declarations
    private external fun mlsCreateGroup(): String
    private external fun mlsJoinGroup(groupId: String): String
    private external fun mlsEncryptMessage(groupId: String, message: String): String
    private external fun mlsDecryptMessage(groupId: String, encryptedMessage: String): String
    private external fun mlsFreeString(ptr: Long)
    
    fun createGroup(): Group {
        val groupId = mlsCreateGroup()
        return Group(
            id = groupId,
            name = "New Group",
            memberIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun joinGroup(groupId: String): Boolean {
        return try {
            val result = mlsJoinGroup(groupId)
            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    fun encryptMessage(groupId: String, message: String): String {
        return mlsEncryptMessage(groupId, message)
    }
    
    fun decryptMessage(groupId: String, encryptedMessage: String): String {
        return mlsDecryptMessage(groupId, encryptedMessage)
    }
}
