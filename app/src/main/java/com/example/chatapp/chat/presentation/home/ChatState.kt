package com.example.chatapp.chat.presentation.home

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.chatapp.chat.domain.models.Chat
import com.example.chatapp.chat.domain.models.ChatPreference
import com.example.chatapp.chat.domain.models.ChatSummary
import com.example.chatapp.chat.domain.models.Mood

@Immutable
data class ChatListState(
    val name: String = "",
    val profilePictureUri: Uri? = null,
    val chats: List<ChatSummary> = emptyList(),
    val isChatListLoading: Boolean = false,
    val isChatDeleting: Boolean = false,
    val selectedChat: ChatState? = null
)

@Immutable
data class ChatState(
    val chat: Chat = Chat(id = "", title = "", messages = emptyList(), createdAt = 0L),
    val chatPreference: ChatPreference = ChatPreference(
        mood = Mood.FRIENDLY,
        isReasoningEnabled = false,
        isOnlineSearchEnabled = false
    ),
    val isAudioRecording: Boolean = false,
    val audioPath: String? = null,
    val shouldGenerateImage: Boolean = false,
    val currentInput: String = "",
    val isChatLoading: Boolean = false,
    val isTranscriptionLoading: Boolean = false,
    val isSelectingImage: Boolean = false,
    val isTyping: Boolean = false,
    @StringRes val imageCompressionError: Int? = null,
    @StringRes val messageLengthError: Int? = null,
    val trendingTopics: List<String> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val selectedImageUri: Uri? = null
)