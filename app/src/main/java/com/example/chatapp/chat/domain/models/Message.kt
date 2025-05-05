package com.example.chatapp.chat.domain.models

data class Message(
    val id: String,
    val role: Role,
    val content: String,
    val image: String?,
    val timestamp: Long
)
