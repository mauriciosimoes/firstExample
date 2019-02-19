package com.example.dao

import com.example.model.Conta
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

import java.io.Closeable
import java.io.File

/**
 * A DAO Facade interface for the Database. This allows to provide several implementations.
 *
 * In this case this is used to provide a Database-based implementation using Exposed,
 * and a cache implementation composing another another DAOFacade.
 */
interface DAOFacade : Closeable {
    /**
     * Initializes all the required data.
     * In this case this should initialize the Users and Kweets tables.
     */
    fun init()

    fun conta(contaId: Int): Conta?

    fun conta(): ListIterator<Conta>

    fun createConta(text: String, isDefaut: Boolean): Int

    // TODO retornar varios.Com o efeito de like
//    fun contaByText(text: String): Conta?

//
}

/**
 * Database implementation of the facade.
 * Uses Exposed, and either an in-memory H2 database or a file-based H2 database by default.
 * But can be configured.
 */
class DAOFacadeDatabase(val db: Database = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")):
        DAOFacade {
    constructor(dir: File) : this(Database.connect("jdbc:h2:file:${dir.canonicalFile.absolutePath}", driver = "org.h2.Driver"))

    override fun init() {
        // Create the used tables
        db.transaction {
            create(Contas, RegistrosDeCompra)
        }
    }

    override fun conta(contaId: Int) = db.transaction {
        Contas.select { Contas.id.eq(contaId) }
            .map { Conta(contaId, it[Contas.text], it[Contas.isDefault]) }
            .singleOrNull()
    }

    override fun conta() = db.transaction {
        Contas.selectAll()
            .map { Conta(it[Contas.id], it[Contas.text], it[Contas.isDefault]) }
            .listIterator()
    }

    override fun createConta(text: String,isDefaut: Boolean): Int {
        return db.transaction {
            Contas.insert {
                it[Contas.text] = text
                it[Contas.isDefault] = isDefaut
            }.generatedKey ?: throw IllegalStateException("No generated key returned")
        }
    }



//    override fun contaByText(text: String) = db.transaction {
//        Contas.select { Contas.text.eq(text) }
//            .map { Conta(it[Contas.id], text, it[Contas.isDefault]) }
//            .singleOrNull()
//    }




    override fun close() {
    }
}
