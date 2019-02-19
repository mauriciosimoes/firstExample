package com.example.loginRegister

import java.util.*


class LoginRegister(val user: String, val password: String)

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)
