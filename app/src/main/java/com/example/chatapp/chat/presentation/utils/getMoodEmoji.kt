package com.example.chatapp.chat.presentation.utils

import com.example.chatapp.chat.domain.models.Mood

fun getMoodEmoji(mood: Mood): String {
    return when (mood) {
        Mood.FRIENDLY -> "🤝"
        Mood.FORMAL -> "💼"
        Mood.CASUAL -> "😄"
        Mood.NERDY -> "🧠"
        Mood.SARCASTIC -> "🎮"
    }
}