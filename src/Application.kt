package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import java.text.DateFormat
import java.util.*


// TODO introduzir modularização
// TODO introduzir teste unitario
// TODO introduzir autenticacao do google
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


// Autenticação


open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

class LoginRegister(val user: String, val password: String)

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)


// Objetos de negocio


data class Snippet(val text: String)

val snippets = Collections.synchronizedList(mutableListOf(
    Snippet("hello"),
    Snippet("world")
))

data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}

data class Conta(val text: String, val isPadrao: Boolean)

val contas = Collections.synchronizedList(mutableListOf(
    Conta("nubank", true)
    , Conta("din", false)
    , Conta("bb", false)
    , Conta("ourocard", false)
))

//data class PostSnippet(val snippet: PostSnippet.Text) {
//    data class Text(val text: String)
//}
data class OQueFoiComprado(val oQueFoiComprado: String)
data class QuantoFoi(val quantoFoi: Double)
data class QuantasVezes(val quantasVezes: Long?) // automaticamente será 1
data class Tag(val tag: String?) // será null
data class ValorDaParcela(val valorDaParcela: Double?) // sera QuantoFoi / QuantasVezes
data class SaiuDeonde(val saiuDeonde: String?) // sera o ultimo utilizado ou marcada como padrao
data class DataDaCompra(val dataDaCompra: Date?) // sera dia corrente
data class UrlNfe(val urlNfe: String?) // sera vazio mesmo


// Modulo principal


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
//            enable(...)
            dateFormat = DateFormat.getDateInstance()
//            disableDefaultTyping()
//            convertValue(..., ...)
//            ...
        }
    }

    val simpleJwt = SimpleJWT("my-super-secret-for-jwt")
    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    routing {
        post("/login-register") {
            val post = call.receive<LoginRegister>() // TODO acrescentar tratamento caso o cliente não envie LoginRegister
            val user = users.getOrPut(post.user) { User(post.user, post.password) }
            if (user.password != post.password) error("Invalid credentials")
            call.respond(mapOf("token" to simpleJwt.sign(user.name)))
        }

        route("/snippets") {
            get {
                //            call.respondText("OK")
                call.respond(mapOf(
                    "snippets" to synchronized(snippets) { snippets.toList() }
                ))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    snippets += Snippet(post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }
        }

        route("/contas") {
            get {
                call.respond(mapOf(
                    "contas" to synchronized(contas) { contas.toList() }
                ))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    snippets += Snippet(post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }
        }
    }
}

