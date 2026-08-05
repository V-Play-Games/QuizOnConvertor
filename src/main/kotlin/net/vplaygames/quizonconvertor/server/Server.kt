package net.vplaygames.quizonconvertor.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*

fun startServer(port: Int = 8080, wait: Boolean = true): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    println("Starting QuizOnConvertor Ktor Web Server on http://localhost:$port ...")
    val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            configureRoutes()
        }
    }
    return server.start(wait = wait)
}
