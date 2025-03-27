package com.example.chatapp.chat.presentation.settings

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue

@Immutable
data class SettingsState(
    val isProfileUpdating: Boolean = false,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val displayName: TextFieldValue = TextFieldValue(),
    val photoUri: Uri? = null,
    @StringRes val displayNameError: Int? = null,
    @StringRes val photoUriError: Int? = null
)