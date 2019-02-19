package com.example.model

import java.io.Serializable

data class Conta(val contaId: Int, val text: String, val isDefaut: Boolean) : Serializable

data class PostConta( val contasDoPost: List<ContaDoPost>) {
    data class ContaDoPost(val text: String, val isDefaut: Boolean)
}
