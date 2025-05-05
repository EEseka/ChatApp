package com.example.chatapp.authentication.presentation.signup

import android.app.Activity
import android.net.Uri

sealed interface SignUpEvents {
    data class OnSetActivityContext(val activityContext: Activity) : SignUpEvents
    data object OnClearActivityContext : SignUpEvents
    data class OnEmailChanged(val email: String) : SignUpEvents
    data class OnPasswordChanged(val password: String) : SignUpEvents
    data class OnRepeatedPasswordChanged(val repeatedPassword: String) : SignUpEvents
    data object OnSignUpClicked : SignUpEvents
    data object OnSignInWithGoogleClicked : SignUpEvents
    data object OnEmailVerifiedClicked : SignUpEvents
    data object ClearEmailVerificationError : SignUpEvents
    data class OnDisplayNameChanged(val name: String) : SignUpEvents
    data class OnPhotoSelected(val uri: Uri, val extension: String) : SignUpEvents
    data object OnSaveProfileClicked : SignUpEvents
}