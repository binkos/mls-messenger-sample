package com.messenger.sample

import com.messenger.sample.services.ServerStorage
import com.messenger.sample.shared.models.ChatMembershipStatus
import com.messenger.sample.shared.models.ChatMessage
import com.messenger.sample.shared.models.CreateChatRequest
import com.messenger.sample.shared.models.CreateJoinRequestRequest
import com.messenger.sample.shared.models.JoinRequest
import com.messenger.sample.shared.models.SendMessageRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json


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
                    val userId = call.request.header("X-User-ID") // Get user ID from header
                    runBlocking {
                        val newChat = ServerStorage.createChat(request.name, userId)
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

            // Mark chat as read
            post("/api/chats/{chatId}/mark-read") {
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    ServerStorage.markChatAsRead(chatId)
                    call.respond(HttpStatusCode.OK, "Chat marked as read")
                }
            }

            // Get chats with user status for a specific user
            get("/api/users/{userId}/chats") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    val chatsWithStatus = ServerStorage.getChatsWithUserStatus(userId)
                    call.respond(chatsWithStatus)
                }
            }

            // Get user status for a specific chat
            get("/api/users/{userId}/chats/{chatId}/status") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val chatId = call.parameters["chatId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                runBlocking {
                    val status = ServerStorage.getUserChatStatus(userId, chatId)
                    call.respond(mapOf("status" to status))
                }
            }

            // Request to join a chat
            post("/api/users/{userId}/chats/{chatId}/join-request") {
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                val chatId = call.parameters["chatId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                try {
                    val request = call.receive<CreateJoinRequestRequest>()
                    runBlocking {
                        // Create join request
                        val joinRequest = JoinRequest(
                            id = "req_${System.currentTimeMillis()}",
                            userName = request.userName,
                            keyPackage = request.keyPackage,
                            groupId = request.groupId,
                            timestamp = System.currentTimeMillis()
                        )
                        ServerStorage.addJoinRequest(joinRequest)

                        // Update user status to PENDING
                        ServerStorage.setUserChatStatus(userId, chatId, ChatMembershipStatus.PENDING)

                        call.respond(HttpStatusCode.Created, joinRequest)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request: ${e.message}")
                }
            }

            // Accept join request (admin function)
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
                        val chatId = foundChatId // Non-null assertion
                        val request = foundRequest // Non-null assertion
                        ServerStorage.removeJoinRequest(chatId, requestId)

                        // Update user status to MEMBER
                        ServerStorage.setUserChatStatus(request.userName, chatId, ChatMembershipStatus.MEMBER)

                        call.respond(HttpStatusCode.OK, "Join request accepted")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Join request not found")
                    }
                }
            }

            // Decline join request (admin function)
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
                        val chatId = foundChatId // Non-null assertion
                        val request = foundRequest // Non-null assertion
                        ServerStorage.removeJoinRequest(chatId, requestId)

                        // Update user status back to NOT_MEMBER
                        ServerStorage.setUserChatStatus(request.userName, chatId, ChatMembershipStatus.NOT_MEMBER)

                        call.respond(HttpStatusCode.OK, "Join request declined")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Join request not found")
                    }
                }
            }
        }
    }.start(wait = true)
}
