package com.example.chatapp.chat.domain

import com.aallam.openai.api.audio.AudioResponseFormat
import com.example.chatapp.chat.domain.models.ChatPreference
import com.example.chatapp.chat.domain.models.Message
import com.example.chatapp.core.domain.util.NetworkError
import com.example.chatapp.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.util.Locale

interface AiDataSource {
    suspend fun chatWithAi(
        prompt: List<Message>,
        preference: ChatPreference
    ): Result<Flow<String>, NetworkError>

    suspend fun generateImage(prompt: String): Result<List<String>, NetworkError>
    suspend fun transcribeAudio(
        audioPath: String,
        language: String = Locale.getDefault().language.ifEmpty { "en" },
        responseFormat: AudioResponseFormat = AudioResponseFormat.Text
    ): Result<String, NetworkError>

    suspend fun getTrendingSearches(): Result<List<String>, NetworkError>
    suspend fun generateChatTitle(chatContent: String): Result<String, NetworkError>
}