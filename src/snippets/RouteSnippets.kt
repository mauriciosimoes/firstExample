package com.example.snippets

import com.example.UrlSnippets
import com.example.UrlSnippets_id
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.routeSnippets() {
    get<UrlSnippets> {
        call.respond(mapOf(
            "snippets" to synchronized(snippets) { snippets.toList() }
        ))
    }

    get<UrlSnippets_id> {
        val snippetId: Int? = it.id

        if (snippetId != null ) {
            call.respond(mapOf(
                "OK" to true
            ))
        } else {
            call.respond(mapOf(
                "OK" to false
            ))
        }
    }
    authenticate {
        post<UrlSnippets> {
            val post = call.receive<PostSnippet>()
            snippets += Snippet(post.snippet.text)
            call.respond(mapOf("OK" to true))
        }
    }
}
