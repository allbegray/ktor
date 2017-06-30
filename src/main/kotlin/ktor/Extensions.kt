package ktor

import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.pipeline.PipelineInterceptor
import org.jetbrains.ktor.routing.HttpMethodRouteSelector
import org.jetbrains.ktor.routing.OrRouteSelector
import org.jetbrains.ktor.routing.Route
import org.jetbrains.ktor.routing.route

fun Route.getOrPost(path: String, body: PipelineInterceptor<ApplicationCall>): Route {
    val selector = OrRouteSelector(HttpMethodRouteSelector(HttpMethod.Get), HttpMethodRouteSelector(HttpMethod.Post))
    return select(selector).route(path) { handle(body) }
}