package com.example.chatapp.chat.presentation.settings

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue

sealed interface SettingsEvents {
    data class OnDisplayNameChanged(val name: TextFieldValue) : SettingsEvents
    data class OnPhotoSelected(val uri: Uri, val extension: String) : SettingsEvents
    data object OnUpdateProfileClicked : SettingsEvents
    data object OnSignOutClicked : SettingsEvents
    data object OnDeleteAccountClicked : SettingsEvents
    data object OnScreenLeave : SettingsEvents
    data object OnScreenReturn : SettingsEvents
}