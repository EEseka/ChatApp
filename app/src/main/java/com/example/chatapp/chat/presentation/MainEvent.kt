package com.example.chatapp.chat.presentation

import com.example.chatapp.core.domain.util.FirebaseError

sealed interface MainEvent {
    data class Error(val error: FirebaseError) : MainEvent
    data object ProfileUpdateComplete : MainEvent
    data object SignOutComplete : MainEvent
    data object AccountDeletionComplete : MainEvent
    data object EmailUpdated : MainEvent
}