package com.example.dao

import org.jetbrains.exposed.sql.Table

/**
 * Represents the Contas table using Exposed as DAO.
 */
object Contas : Table() {
    val id = integer("id").primaryKey().autoIncrement()
    val text = varchar("text", 20).uniqueIndex()
    val isDefault = bool("isDefault")
}

