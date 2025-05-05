package com.example.chatapp.chat.domain.models

data class Chat(
    val id: String,
    val title: String,
    val messages: List<Message>,
    val createdAt: Long
)