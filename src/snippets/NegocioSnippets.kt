package com.example.snippets

import java.util.*


data class Snippet(val text: String)

val snippets = Collections.synchronizedList(mutableListOf(
    Snippet("hello"),
    Snippet("world")
))

data class PostSnippet(val snippet: PostSnippet.Text) {
    data class Text(val text: String)
}
