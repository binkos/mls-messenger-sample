package com.messenger.sample.native

import com.messenger.sample.shared.models.Group
import com.messenger.sample.shared.models.Message
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class MlsResponse(
    val group_id: String? = null,
    val epoch: Long? = null,
    val status: String? = null,
    val encrypted: String? = null,
    val decrypted: String? = null,
    val member_count: Int? = null,
    val error: String? = null
)

class MlsNativeWrapper {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private var nativeLibraryLoaded = false
        
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
                
                System.loadLibrary(libraryName)
                nativeLibraryLoaded = true
                println("✅ Native MLS library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                println("⚠️  Warning: Native MLS library not found. Using fallback implementation.")
                println("   To enable MLS features, install Rust and build the native library:")
                println("   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh")
                println("   cd native && cargo build --release")
                nativeLibraryLoaded = false
            }
        }
    }
    
    // Native method declarations
    private external fun mlsCreateGroup(): String
    private external fun mlsJoinGroup(groupId: String): String
    private external fun mlsEncryptMessage(groupId: String, message: String): String
    private external fun mlsDecryptMessage(groupId: String, encryptedMessage: String): String
    private external fun mlsGetGroupInfo(groupId: String): String
    private external fun mlsFreeString(ptr: Long)
    
    fun createGroup(): Group {
        if (!nativeLibraryLoaded) {
            // Fallback implementation
            return Group(
                id = java.util.UUID.randomUUID().toString(),
                name = "New Group (Fallback)",
                memberIds = emptyList(),
                createdAt = System.currentTimeMillis()
            )
        }
        
        val response = mlsCreateGroup()
        val mlsResponse = json.decodeFromString<MlsResponse>(response)
        
        if (mlsResponse.error != null) {
            throw RuntimeException("Failed to create group: ${mlsResponse.error}")
        }
        
        return Group(
            id = mlsResponse.group_id ?: throw RuntimeException("No group ID returned"),
            name = "New Group",
            memberIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun joinGroup(groupId: String): Boolean {
        if (!nativeLibraryLoaded) {
            // Fallback implementation - always succeed
            return true
        }
        
        return try {
            val response = mlsJoinGroup(groupId)
            val mlsResponse = json.decodeFromString<MlsResponse>(response)
            mlsResponse.error == null && mlsResponse.status == "joined"
        } catch (e: Exception) {
            false
        }
    }
    
    fun encryptMessage(groupId: String, message: String): String {
        if (!nativeLibraryLoaded) {
            // Fallback implementation - simple base64 encoding
            return java.util.Base64.getEncoder().encodeToString(message.toByteArray())
        }
        
        val response = mlsEncryptMessage(groupId, message)
        val mlsResponse = json.decodeFromString<MlsResponse>(response)
        
        if (mlsResponse.error != null) {
            throw RuntimeException("Failed to encrypt message: ${mlsResponse.error}")
        }
        
        return mlsResponse.encrypted ?: throw RuntimeException("No encrypted message returned")
    }
    
    fun decryptMessage(groupId: String, encryptedMessage: String): String {
        if (!nativeLibraryLoaded) {
            // Fallback implementation - simple base64 decoding
            return try {
                String(java.util.Base64.getDecoder().decode(encryptedMessage))
            } catch (e: Exception) {
                encryptedMessage // Return as-is if decoding fails
            }
        }
        
        val response = mlsDecryptMessage(groupId, encryptedMessage)
        val mlsResponse = json.decodeFromString<MlsResponse>(response)
        
        if (mlsResponse.error != null) {
            throw RuntimeException("Failed to decrypt message: ${mlsResponse.error}")
        }
        
        return mlsResponse.decrypted ?: throw RuntimeException("No decrypted message returned")
    }
    
    fun getGroupInfo(groupId: String): Group? {
        if (!nativeLibraryLoaded) {
            // Fallback implementation - return a basic group
            return Group(
                id = groupId,
                name = "Group $groupId (Fallback)",
                memberIds = listOf("user_1", "user_2"),
                createdAt = System.currentTimeMillis()
            )
        }
        
        return try {
            val response = mlsGetGroupInfo(groupId)
            val mlsResponse = json.decodeFromString<MlsResponse>(response)
            
            if (mlsResponse.error != null) {
                return null
            }
            
            Group(
                id = mlsResponse.group_id ?: return null,
                name = "Group ${mlsResponse.group_id}",
                memberIds = List(mlsResponse.member_count ?: 0) { "member_$it" },
                createdAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
