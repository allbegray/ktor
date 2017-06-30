package ktor.routers

import ktor.LoginForm
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.freemarker.FreeMarkerContent
import org.jetbrains.ktor.locations.get
import org.jetbrains.ktor.locations.post
import org.jetbrains.ktor.routing.Route

fun Route.login() {
    get<LoginForm> {
        call.respond(FreeMarkerContent("login.ftlh", mapOf<String, Any>()))
    }
    post<LoginForm> {
        println(it.loginId)
        println(it.loginPassword)
    }
}