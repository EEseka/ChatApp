package com.example.chatapp.authentication.presentation.welcome

sealed class WelcomeUiState {
    data object Initial : WelcomeUiState()
    data object Onboarding : WelcomeUiState()
    data object Authenticated : WelcomeUiState()
    data object NotAuthenticated : WelcomeUiState()
    data object Error : WelcomeUiState()
}