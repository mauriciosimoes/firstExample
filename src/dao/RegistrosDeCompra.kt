package com.example.dao

import org.jetbrains.exposed.sql.Table

/**
 * Represents the RegistrosDeCompra table using Exposed as DAO.
 */
object RegistrosDeCompra : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val oQueFoiComprado = varchar("oQueFoiComprado", 50)
    val quantoFoi = decimal("quantoFoi", 6, 2)
    val quantasVezes = integer("quantasVezes")
    val tag = varchar("tag", 20).nullable()
    val valorDaParcela = decimal("valorDaParcela", 6, 2)
    val contaText = varchar("conta_Text", 20)
    val dataDaCompra = datetime("dataDaCompra")
    val urlNfe = varchar("urlNfe", 1024).nullable()
    val usuario = varchar("usuario", 254)
}