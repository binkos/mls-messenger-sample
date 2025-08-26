package com.messenger.sample

import com.messenger.sample.services.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CreateChatRequest(val name: String)

@Serializable
data class SendMessageRequest(val userName: String, val message: String)

@Serializable
data class CreateJoinRequestRequest(
    val userName: String,
    val keyPackage: String,
    val groupId: String
)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        routing {
            // Health check
            get("/health") {
                call.respondText("OK", ContentType.Text.Plain)
            }
            
            // Get all chats
            get("/api/chats") {
                runBlocking {
                    val chats = ServerStorage.getAllChats()
                    call.respond(chats)
                }
            }
            
            // Get specific chat
            get("/api/chats/{chatId}") {
                val chatId = call.parameters["chatId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    val chat = ServerStorage.getChat(chatId)
                    if (chat != null) {
                        call.respond(chat)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
            
            // Create new chat
            post("/api/chats") {
                try {
                    val request = call.receive<CreateChatRequest>()
                    runBlocking {
                        val newChat = ServerStorage.createChat(request.name)
                        call.respond(HttpStatusCode.Created, newChat)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request: ${e.message}")
                }
            }
            
            // Get messages for a chat
            get("/api/chats/{chatId}/messages") {
                val chatId = call.parameters["chatId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    val messages = ServerStorage.getMessages(chatId)
                    call.respond(messages)
                }
            }
            
            // Send message to a chat
            post("/api/chats/{chatId}/messages") {
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                try {
                    val request = call.receive<SendMessageRequest>()
                    runBlocking {
                        val message = ChatMessage(
                            id = "msg_${System.currentTimeMillis()}",
                            userName = request.userName,
                            message = request.message,
                            timestamp = System.currentTimeMillis(),
                            isOwnMessage = false
                        )
                        ServerStorage.addMessage(chatId, message)
                        call.respond(HttpStatusCode.Created, message)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request: ${e.message}")
                }
            }
            
            // Get join requests for a chat
            get("/api/chats/{chatId}/join-requests") {
                val chatId = call.parameters["chatId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    val requests = ServerStorage.getJoinRequests(chatId)
                    call.respond(requests)
                }
            }
            
            // Create join request
            post("/api/chats/{chatId}/join-requests") {
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                try {
                    val request = call.receive<CreateJoinRequestRequest>()
                    runBlocking {
                        val joinRequest = JoinRequest(
                            id = "req_${System.currentTimeMillis()}",
                            userName = request.userName,
                            keyPackage = request.keyPackage,
                            groupId = request.groupId,
                            timestamp = System.currentTimeMillis()
                        )
                        ServerStorage.addJoinRequest(joinRequest)
                        call.respond(HttpStatusCode.Created, joinRequest)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request: ${e.message}")
                }
            }
            
            // Accept join request
            post("/api/join-requests/{requestId}/accept") {
                val requestId = call.parameters["requestId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    // Find the join request first
                    val allChats = ServerStorage.getAllChats()
                    var foundRequest: JoinRequest? = null
                    var foundChatId: String? = null
                    
                    for (chat in allChats) {
                        val requests = ServerStorage.getJoinRequests(chat.id)
                        val request = requests.find { it.id == requestId }
                        if (request != null) {
                            foundRequest = request
                            foundChatId = chat.id
                            break
                        }
                    }
                    
                    if (foundRequest != null && foundChatId != null) {
                        ServerStorage.removeJoinRequest(foundChatId!!, requestId)
                        call.respond(HttpStatusCode.OK, "Join request accepted")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Join request not found")
                    }
                }
            }
            
            // Decline join request
            post("/api/join-requests/{requestId}/decline") {
                val requestId = call.parameters["requestId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    // Find the join request first
                    val allChats = ServerStorage.getAllChats()
                    var foundRequest: JoinRequest? = null
                    var foundChatId: String? = null
                    
                    for (chat in allChats) {
                        val requests = ServerStorage.getJoinRequests(chat.id)
                        val request = requests.find { it.id == requestId }
                        if (request != null) {
                            foundRequest = request
                            foundChatId = chat.id
                            break
                        }
                    }
                    
                    if (foundRequest != null && foundChatId != null) {
                        ServerStorage.removeJoinRequest(foundChatId!!, requestId)
                        call.respond(HttpStatusCode.OK, "Join request declined")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Join request not found")
                    }
                }
            }
            
            // Mark chat as read
            post("/api/chats/{chatId}/mark-read") {
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    ServerStorage.markChatAsRead(chatId)
                    call.respond(HttpStatusCode.OK, "Chat marked as read")
                }
            }
        }
    }.start(wait = true)
}
