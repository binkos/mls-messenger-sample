package com.messenger.sample.backend.plugins

import com.messenger.sample.backend.routes.configureMessageRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.Routing

fun Application.configureRouting() {
    install(Routing) {
        configureMessageRoutes()
    }
}
