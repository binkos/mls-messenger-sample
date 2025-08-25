package com.messenger.sample.desktop.services

import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

//class BackendService {
//    private val client = HttpClient(CIO) {
//        install(ContentNegotiation) {
//            json(Json {
//                ignoreUnknownKeys = true
//            })
//        }
//    }
//
//    private val baseUrl = "http://localhost:8080"
//    private val mlsWrapper = MlsNativeWrapper()
//
//    suspend fun getGroups(): List<Group> {
//        return client.get("$baseUrl/api/groups").body()
//    }
//
//    suspend fun createGroup(group: Group): Group {
//        println("🚀 Creating group: ${group.name}")
//
//        // First create MLS group locally
//        val mlsGroup = try {
//            println("🔐 Creating MLS group locally...")
//            val result = mlsWrapper.createGroup()
//            println("✅ MLS group created locally with ID: ${result.id}")
//            result
//        } catch (e: Exception) {
//            println("❌ MLS group creation failed: ${e.message}")
//            null
//        }
//
//        // Then create group on server with the MLS group ID if available
//        val groupToCreate = if (mlsGroup != null) {
//            println("📡 Creating server group with MLS ID: ${mlsGroup.id}")
//            group.copy(id = mlsGroup.id)
//        } else {
//            println("📡 Creating server group with generated ID")
//            group
//        }
//
//        val serverGroup = client.post("$baseUrl/api/groups") {
//            contentType(ContentType.Application.Json)
//            setBody(groupToCreate)
//        }.body<Group>()
//
//        println("✅ Server group created with ID: ${serverGroup.id}")
//        return serverGroup
//    }
//
//    suspend fun getMessages(groupId: String): List<Message> {
//        return client.get("$baseUrl/api/messages?groupId=$groupId").body()
//    }
//
//    suspend fun sendMessage(message: Message): Message {
//        // Encrypt the message content on client side before sending
//        val encryptedContent = encryptMessage(message.content, message.groupId)
//        val encryptedMessage = message.copy(
//            content = encryptedContent
//        )
//
//        return client.post("$baseUrl/api/messages") {
//            contentType(ContentType.Application.Json)
//            setBody(encryptedMessage)
//        }.body()
//    }
//
//    private fun encryptMessage(content: String, groupId: String): String {
//        return try {
//            println("🔐 Attempting MLS encryption for group: $groupId")
//            val result = mlsWrapper.encryptMessage(groupId, content)
//            println("✅ MLS encryption successful")
//            result
//        } catch (e: Exception) {
//            // Fallback to base64 if MLS encryption fails
//            println("❌ MLS encryption failed, using fallback: ${e.message}")
//            println("   Group ID: $groupId")
//            println("   Content length: ${content.length}")
//            java.util.Base64.getEncoder().encodeToString(content.toByteArray())
//        }
//    }
//
//    fun decryptMessage(encryptedContent: String, groupId: String): String {
//        return try {
//            println("🔓 Attempting MLS decryption for group: $groupId")
//            val result = mlsWrapper.decryptMessage(groupId, encryptedContent)
//            println("✅ MLS decryption successful")
//            result
//        } catch (e: Exception) {
//            // Fallback to base64 if MLS decryption fails
//            println("❌ MLS decryption failed, using fallback: ${e.message}")
//            println("   Group ID: $groupId")
//            println("   Content length: ${encryptedContent.length}")
//            try {
//                String(java.util.Base64.getDecoder().decode(encryptedContent))
//            } catch (e2: Exception) {
//                encryptedContent // Return as-is if all decoding fails
//            }
//        }
//    }
//
//    suspend fun joinGroup(groupId: String): Boolean {
//        return try {
//            val response = client.post("$baseUrl/api/groups/$groupId/join").body<Map<String, String>>()
//            response["status"] == "joined"
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    suspend fun getGroupInfo(groupId: String): Group? {
//        return try {
//            client.get("$baseUrl/api/groups/$groupId").body()
//        } catch (e: Exception) {
//            null
//        }
//    }
//}
