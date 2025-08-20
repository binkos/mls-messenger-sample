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

class BackendService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    private val baseUrl = "http://localhost:8080"
    
    suspend fun getGroups(): List<Group> {
        return client.get("$baseUrl/api/groups").body()
    }
    
    suspend fun createGroup(group: Group): Group {
        return client.post("$baseUrl/api/groups") {
            contentType(ContentType.Application.Json)
            setBody(group)
        }.body()
    }
    
    suspend fun getMessages(groupId: String): List<Message> {
        return client.get("$baseUrl/api/messages?groupId=$groupId").body()
    }
    
    suspend fun sendMessage(message: Message): Message {
        return client.post("$baseUrl/api/messages") {
            contentType(ContentType.Application.Json)
            setBody(message)
        }.body()
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
