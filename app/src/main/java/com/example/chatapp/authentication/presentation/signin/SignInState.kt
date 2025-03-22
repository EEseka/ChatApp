package com.example.chatapp.authentication.presentation.signin

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class SignInState(
    val isLoading: Boolean = false,
    val email: String = "",
    val password: String = "",
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    val isEmailVerified: Boolean = false,
    @StringRes val emailVerificationError: Int? = null,
    // For Reset Password
    val forgotPasswordEmail: String = "",
    @StringRes val forgotPasswordEmailError: Int? = null,
    val forgotPasswordEmailSent: Boolean = false,
)