package com.example.chatapp.authentication.presentation.signin

import android.app.Activity

sealed interface SignInEvents {
    data class OnEmailChanged(val email: String) : SignInEvents
    data class OnPasswordChanged(val password: String) : SignInEvents
    data class OnForgotPasswordEmailChanged(val email: String) : SignInEvents
    data object OnSignInClicked : SignInEvents
    data object OnSignInWithGoogleClicked : SignInEvents
    data object OnResendVerificationEmailClicked : SignInEvents
    data object OnEmailVerifiedClicked : SignInEvents
    data object ClearEmailVerificationError : SignInEvents
    data object ClearForgotPasswordError : SignInEvents
    data object ClearForgotPasswordEmailSent : SignInEvents
    data object OnSendPasswordResetClicked : SignInEvents
    data object OnAutomaticSignInInitiated : SignInEvents
    data class OnSetActivityContext(val activityContext: Activity) : SignInEvents
    data object OnClearActivityContext : SignInEvents
}