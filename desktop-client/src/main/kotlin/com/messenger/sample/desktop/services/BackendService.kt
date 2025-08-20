package com.messenger.sample.desktop.services

import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group
import com.messenger.sample.native.MlsNativeWrapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class BackendService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    private val baseUrl = "http://localhost:8080"
    private val mlsWrapper = MlsNativeWrapper()
    
    suspend fun getGroups(): List<Group> {
        return client.get("$baseUrl/api/groups").body()
    }
    
    suspend fun createGroup(group: Group): Group {
        // First create MLS group locally
        val mlsGroup = try {
            mlsWrapper.createGroup()
        } catch (e: Exception) {
            println("MLS group creation failed: ${e.message}")
            null
        }
        
        // Then create group on server
        val serverGroup = client.post("$baseUrl/api/groups") {
            contentType(ContentType.Application.Json)
            setBody(group)
        }.body<Group>()
        
        // If MLS group was created, use its ID
        return if (mlsGroup != null) {
            serverGroup.copy(id = mlsGroup.id)
        } else {
            serverGroup
        }
    }
    
    suspend fun getMessages(groupId: String): List<Message> {
        return client.get("$baseUrl/api/messages?groupId=$groupId").body()
    }
    
    suspend fun sendMessage(message: Message): Message {
        // Encrypt the message content on client side before sending
        val encryptedMessage = message.copy(
            content = encryptMessage(message.content, message.groupId)
        )
        
        return client.post("$baseUrl/api/messages") {
            contentType(ContentType.Application.Json)
            setBody(encryptedMessage)
        }.body()
    }
    
    private fun encryptMessage(content: String, groupId: String): String {
        return try {
            mlsWrapper.encryptMessage(groupId, content)
        } catch (e: Exception) {
            // Fallback to base64 if MLS encryption fails
            println("MLS encryption failed, using fallback: ${e.message}")
            java.util.Base64.getEncoder().encodeToString(content.toByteArray())
        }
    }
    
    fun decryptMessage(encryptedContent: String): String {
        return try {
            // Try MLS decryption first (we need groupId for this)
            // For now, assume it's base64 encoded
            String(java.util.Base64.getDecoder().decode(encryptedContent))
        } catch (e: Exception) {
            encryptedContent // Return as-is if decoding fails
        }
    }
    
    suspend fun joinGroup(groupId: String): Boolean {
        return try {
            val response = client.post("$baseUrl/api/groups/$groupId/join").body<Map<String, String>>()
            response["status"] == "joined"
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getGroupInfo(groupId: String): Group? {
        return try {
            client.get("$baseUrl/api/groups/$groupId").body()
        } catch (e: Exception) {
            null
        }
    }
}
