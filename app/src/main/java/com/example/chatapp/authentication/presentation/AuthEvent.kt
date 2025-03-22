package com.example.chatapp.authentication.presentation

import com.example.chatapp.core.domain.util.FirebaseError

sealed interface AuthEvent {
    data object SignUpSuccess : AuthEvent
    data object SignInSuccess : AuthEvent
    data object EmailVerified : AuthEvent
    data object ProfileSetupComplete : AuthEvent
    data class Error(val error: FirebaseError) : AuthEvent
}