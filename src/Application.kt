package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.dao.DAOFacade
import com.example.dao.DAOFacadeCache
import com.example.dao.DAOFacadeDatabase
import com.example.loginRegister.routeLoginRegister
import com.example.model.Conta
import com.example.model.PostConta
import com.example.snippets.routeSnippets
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.mchange.v2.c3p0.ComboPooledDataSource
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.auth.principal
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import org.jetbrains.exposed.sql.Database
import java.io.File
import java.io.IOException
import java.sql.Driver
import java.text.DateFormat
import java.util.*


// TODO introduzir modularização
// TODO introduzir teste unitario
// TODO tratamento de Erro
// TODO introduzir autenticacao do google
// TODO substituir objetospor banco de dados: contas, registrosDeCompra


@KtorExperimentalLocationsAPI
@Location ("/login-register") class UrlLoginRegister
@KtorExperimentalLocationsAPI
@Location("/snippets") class UrlSnippets
@KtorExperimentalLocationsAPI
@Location("/snippets/{id}") class UrlSnippets_id(val id: Int)

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

/**
 * Trecho relacionado a [NegocioLoginRegister]
 */
open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}


/**
 * Trecho relacionado a [Exceptions]
 */
class InvalidCredentialsException(message: String) : RuntimeException(message)
class NegocioException(message: String) : RuntimeException(message)


// Objetos de negocio


//val contas = Collections.synchronizedList(mutableListOf(
//    Conta(1,"nubank", true)
//    , Conta(2, "din", false)
//    , Conta(3, "bb", false)
//    , Conta(4, "ourocard", false)
//))

data class RegistroDeCompra(
    val oQueFoiComprado: String
    , val quantoFoi: Double
    , val quantasVezes: Long? // automaticamente será 1
    , val tag: String? // será null
    , val valorDaParcela: Double? // sera QuantoFoi / QuantasVezes
    , val contaText: String? // sera o ultimo utilizado ou marcada como padrao
    , val dataDaCompra: Date? // sera dia corrente
    , val urlNfe: String? // sera vazio mesmo
    , val usuario: String
)

val registrosDeCompra = Collections.synchronizedList(mutableListOf(
    RegistroDeCompra("nada"
        , 0.0
        , null
        , null
        , null
        , null
        , null
        , null
        , "anomino")
))

data class PostRegistrosDeCompra( val registroDeCompraDoPost: RegistroDeCompraDoPost) {
    data class RegistroDeCompraDoPost (
        val oQueFoiComprado: String
        , val quantoFoi: Double
        , val quantasVezes: Long? // automaticamente será 1
        , val tag: String? // será null
        , val valorDaParcela: Double? // sera QuantoFoi / QuantasVezes
        , val contaText: String? // sera o ultimo utilizado ou marcada como padrao
        , val dataDaCompra: Date? // sera dia corrente
        , val urlNfe: String? // sera vazio mesmo
    )
}


// Modulo principal


@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
fun Application.module(testing: Boolean = false) {
    // Obtains the youkube config key from the application.conf file.
    // Inside that key, we then read several configuration properties
    // with the [session.cookie], the [key] or the [upload.dir]
    val registroDeComprasConfig = environment.config.config("registroDeCompras")
//    val sessionCookieConfig = youkubeConfig.config("session.cookie")
//    val key: String = sessionCookieConfig.property("key").getString()
//    val sessionkey = hex(key)

    // We create the folder and a [Database] in that folder for the configuration [upload.dir].
    val uploadDirPath: String = registroDeComprasConfig.property("upload.dir").getString()
    val uploadDir = File(uploadDirPath)
    if (!uploadDir.mkdirs() && !uploadDir.exists()) {
        throw IOException("Failed to create directory ${uploadDir.absolutePath}")
    }

    val cacheDirPath: String = registroDeComprasConfig.property("cache.dir").getString()
    val cacheDir = File(cacheDirPath)
    if (!cacheDir.mkdirs() && !cacheDir.exists()) {
        throw IOException("Failed to create directory ${cacheDir.absolutePath}")
    }

    //val database = Database(uploadDir)

    /**
     * Pool of JDBC connections used.
     */
    val pool = ComboPooledDataSource().apply {
        driverClass = Driver::class.java.name
        jdbcUrl = "jdbc:h2:file:${uploadDir.canonicalFile.absolutePath}"
        user = ""
        password = ""
    }

    /**
     * Constructs a facade with the database, connected to the DataSource configured earlier with the [dir]
     * for storing the database.
     */
    log.debug("teste mauricio ${cacheDir.parentFile}")
    val dao: DAOFacade = DAOFacadeCache(
            DAOFacadeDatabase(Database.connect(pool))
            , File(cacheDir.parentFile, "ehcache"))

    // First we initialize the database.
    dao.init()

    // And we subscribe to the stop event of the application, so we can also close the [ComboPooledDataSource] [pool].
    environment.monitor.subscribe(ApplicationStopped) { pool.close() }

    // Now we call to a main with the dependencies as arguments.
    // Separating this function with its dependencies allows us to provide several modules with
    // the same code and different datasources living in the same application,
    // and to provide mocked instances for doing integration tests.
    mainWithDependencies(dao)
}


@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
fun Application.mainWithDependencies(dao: DAOFacade) {
    // This adds automatically Date and Server headers to each response, and would allow you to configure
    // additional headers served to each response.
    install(DefaultHeaders)

    // This uses use the logger to log every call (request/response)
    install(CallLogging)

    // Allows to use classes annotated with @Location to represent URLs.
    // They are typed, can be constructed to generate URLs, and can be used to register routes.
    install(Locations)

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

    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
        exception<NegocioException> { exception ->
            call.respond(HttpStatusCode.BadRequest, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }

    routing {
        routeLoginRegister(simpleJwt)
        routeSnippets()

       route("/contas") {
           get {
               call.respond(mapOf(
                   "contas" to dao.conta()
               ))
           }
            get("{id}") {
                val idString = call.parameters["id"] ?: throw BadRequestException("Parametro ID não definido")
                val id: Int = idString.toIntOrNull() ?:  throw NegocioException("Parametro ID de CONTA não definido")

                call.respond(mapOf(
                    "conta" to dao.conta(id)
                ))
            }
           authenticate {
               post {
                   val userIdPrincipal = call.principal<UserIdPrincipal>() ?:
                        error("Informação de autenticação não encontrado")

                   val post = try { call.receive<PostConta>() }
                        catch (e: JsonParseException) { throw NegocioException( e.toString() ) }
                        catch (e: MismatchedInputException) { throw NegocioException( e.toString() ) }
                        catch (e: MissingKotlinParameterException) { throw NegocioException( e.msg ) }
                   val contasMutableList = arrayListOf<Conta>()

                   post.contasDoPost.forEach {
                       val id: Int = dao.createConta(it.text, it.isDefaut)
                       contasMutableList.add( Conta(id, it.text, it.isDefaut) )
                   }

                   call.respond(mapOf("OK" to true, "contas" to contasMutableList))
               }
           }
       }

        route("/registrosDeCompra") {
            get {
                call.respond(mapOf(
                    "registrosDeCompra" to synchronized(registrosDeCompra) { registrosDeCompra.toList() }
                ))
            }

            authenticate {
                post {
                    val userIdPrincipal = call.principal<UserIdPrincipal>() ?: error("No principal")

                    val post = call.receive<PostRegistrosDeCompra>()
                    registrosDeCompra += RegistroDeCompra(post.registroDeCompraDoPost.oQueFoiComprado
                        , post.registroDeCompraDoPost.quantoFoi
                        , post.registroDeCompraDoPost.quantasVezes
                        , post.registroDeCompraDoPost.tag
                        , post.registroDeCompraDoPost.valorDaParcela
                        , post.registroDeCompraDoPost.contaText
                        , post.registroDeCompraDoPost.dataDaCompra
                        , post.registroDeCompraDoPost.urlNfe
                        , userIdPrincipal.name)
                    call.respond(mapOf("OK" to true))
                }
            }
        }
    }
}