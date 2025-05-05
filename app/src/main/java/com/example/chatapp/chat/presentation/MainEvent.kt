package com.example.chatapp.chat.presentation

import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.FirebaseFirestoreError
import com.example.chatapp.core.domain.util.NetworkError

sealed interface MainEvent {
    data class AuthError(val error: FirebaseAuthError) : MainEvent
    data class NetworkingError(val error: NetworkError) : MainEvent
    data class DatabaseError(val error: FirebaseFirestoreError) : MainEvent
    data object ProfileUpdateComplete : MainEvent
    data object SignOutComplete : MainEvent
    data object AccountDeletionComplete : MainEvent
    data object EmailUpdated : MainEvent
}