package com.example.chatapp.chat.presentation.home

import android.net.Uri
import com.example.chatapp.chat.domain.models.Mood

sealed class ChatEvents {
    data class OnDeleteChat(val chatId: String) : ChatEvents()
    data class OnChatSelected(val chatId: String) : ChatEvents()
    data object OnCreateNewChat : ChatEvents()
    data object OnMessageSent : ChatEvents()
    data class OnInputChanged(val input: String) : ChatEvents()
    data class OnImageSelected(val uri: Uri, val extension: String) : ChatEvents()
    data object OnRemoveImage : ChatEvents()
    data class OnTrendingTopicSelected(val topic: String) : ChatEvents()
    data class OnChatTitleChanged(val title: String) : ChatEvents()
    data object OnSaveTitleEdit : ChatEvents()
    data class OnMoodSelected(val mood: Mood) : ChatEvents()
    data object OnToggleReasoning : ChatEvents()
    data object OnToggleOnlineSearch : ChatEvents()
    data object OnToggleImageGeneration : ChatEvents()
    data class OnEditMessage(val messageId: String) : ChatEvents()
    data class OnEditedMessageSent(val messageId: String) : ChatEvents()
    data object OnStartAudioRecording : ChatEvents()
    data object OnStopAudioRecording : ChatEvents()
    data object OnCancelAudioRecording : ChatEvents()
}