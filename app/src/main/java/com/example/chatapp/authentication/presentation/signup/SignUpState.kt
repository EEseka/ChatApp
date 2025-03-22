package com.example.chatapp.authentication.presentation.signup

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class SignUpState(
    val isLoading: Boolean = false,
    val email: String = "",
    @StringRes val emailError: Int? = null,
    val password: String = "",
    @StringRes val passwordError: Int? = null,
    val repeatedPassword: String = "",
    @StringRes val repeatedPasswordError: Int? = null,
    val isEmailVerified: Boolean = false,
    @StringRes val emailVerificationError: Int? = null,
    // For User Profile
    val displayName: String = "",
    val photoUri: Uri? = null,
    @StringRes val displayNameError: Int? = null,
    @StringRes val photoUriError: Int? = null
)