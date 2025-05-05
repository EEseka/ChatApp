package com.example.chatapp.chat.domain.models

data class ChatPreference(
    val mood: Mood,
    val isReasoningEnabled: Boolean,
    val isOnlineSearchEnabled: Boolean
)