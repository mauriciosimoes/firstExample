package com.example.loginRegister

import com.example.*
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.routeLoginRegister(simpleJwt: SimpleJWT) {
    post<UrlLoginRegister> {
        val post = try { call.receive<LoginRegister>() }
                catch (e: JsonParseException) { throw NegocioException( e.toString() ) }
                catch (e: MismatchedInputException) { throw NegocioException( e.toString() ) }
                catch (e: MissingKotlinParameterException) { throw NegocioException( e.msg ) }

        val user = users.getOrPut(post.user) { User(post.user, post.password) }
//            if (user.password != post.password) error("Invalid credentials")
        if (user.password != post.password) throw InvalidCredentialsException("Invalid credentials")
        call.respond(mapOf("token" to simpleJwt.sign(user.name)))
    }
}
