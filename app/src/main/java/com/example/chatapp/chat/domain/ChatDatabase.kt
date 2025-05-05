package com.example.chatapp.chat.domain

import com.example.chatapp.chat.domain.models.Chat
import com.example.chatapp.chat.domain.models.ChatSummary
import com.example.chatapp.core.domain.util.FirebaseFirestoreError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.auth.FirebaseUser

interface ChatDatabase {
    val user: FirebaseUser?
    suspend fun saveChat(chat: Chat): Result<Unit, FirebaseFirestoreError>
    suspend fun getChatById(chatId: String): Result<Chat, FirebaseFirestoreError>
    suspend fun getAllChats(): Result<List<ChatSummary>, FirebaseFirestoreError>
    suspend fun deleteChat(chatId: String): Result<Unit, FirebaseFirestoreError>
}