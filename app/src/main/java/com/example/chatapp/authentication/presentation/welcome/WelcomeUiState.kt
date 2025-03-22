package com.example.chatapp.authentication.presentation.welcome

sealed class WelcomeUiState {
    object Initial : WelcomeUiState()
    object Onboarding : WelcomeUiState()
    object Authenticated : WelcomeUiState()
    object NotAuthenticated : WelcomeUiState()
    object Error : WelcomeUiState()
}