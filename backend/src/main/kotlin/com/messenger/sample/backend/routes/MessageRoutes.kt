package com.messenger.sample.backend.routes

import com.messenger.sample.backend.services.MessageService
import com.messenger.sample.shared.models.Message
import com.messenger.sample.shared.models.Group
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.configureMessageRoutes() {
    val messageService = MessageService()
    
    route("/api/messages") {
        get {
            val groupId = call.request.queryParameters["groupId"] ?: ""
            val messages = messageService.getMessages(groupId)
            call.respond(messages)
        }
        
        post {
            val message = call.receive<Message>()
            val savedMessage = messageService.saveMessage(message)
            call.respond(HttpStatusCode.Created, savedMessage)
        }
    }
    
    route("/api/groups") {
        get {
            val groups = messageService.getGroups()
            call.respond(groups)
        }
        
        post {
            val group = call.receive<Group>()
            val savedGroup = messageService.createGroup(group)
            call.respond(HttpStatusCode.Created, savedGroup)
        }
        
        get("/{groupId}") {
            val groupId = call.parameters["groupId"] ?: ""
            val group = messageService.getGroupInfo(groupId)
            if (group != null) {
                call.respond(group)
            } else {
                call.respond(HttpStatusCode.NotFound, "Group not found")
            }
        }
        
        post("/{groupId}/join") {
            val groupId = call.parameters["groupId"] ?: ""
            val success = messageService.joinGroup(groupId)
            if (success) {
                call.respond(HttpStatusCode.OK, mapOf("status" to "joined", "groupId" to groupId))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Failed to join group"))
            }
        }
    }
}
