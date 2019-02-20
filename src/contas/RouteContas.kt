package com.example.contas

import com.example.NegocioException
import com.example.UrlContas
import com.example.UrlContas_id
import com.example.dao.DAOFacade
import com.example.model.Conta
import com.example.model.PostConta
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.call
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.principal
import io.ktor.features.BadRequestException
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

@KtorExperimentalLocationsAPI
fun Route.routeContas(dao: DAOFacade) {
    get<UrlContas> {
        call.respond(
            mapOf(
                "contas" to dao.conta()
            )
        )
    }

    get<UrlContas_id> {
        val idString = call.parameters["id"] ?: throw BadRequestException("Parametro ID não definido")
        val id: Int = idString.toIntOrNull() ?: throw NegocioException("Parametro ID de CONTA não definido")

        call.respond(
            mapOf(
                "conta" to dao.conta(id)
            )
        )
    }

    authenticate {
        post<UrlContas> {
            val userIdPrincipal =
                call.principal<UserIdPrincipal>() ?: error("Informação de autenticação não encontrado")

            val post = try {
                call.receive<PostConta>()
            } catch (e: JsonParseException) {
                throw NegocioException(e.toString())
            } catch (e: MismatchedInputException) {
                throw NegocioException(e.toString())
            } catch (e: MissingKotlinParameterException) {
                throw NegocioException(e.msg)
            }
            val contasMutableList = arrayListOf<Conta>()

            post.contasDoPost.forEach {
                val id: Int = dao.createConta(it.text, it.isDefaut)
                contasMutableList.add(Conta(id, it.text, it.isDefaut))
            }

            call.respond(mapOf("OK" to true, "contas" to contasMutableList))
        }
    }
}
