package com.example.chatapp.chat.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chatapp.R

sealed class MainNavDestinations(
    val route: String,
    @StringRes val label: Int,
    val outlinedIcon: ImageVector? = null,
    val filledIcon: ImageVector? = null
) {
    data object Chat : MainNavDestinations(
        "chat", R.string.chat, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat
    )

    data object Settings : MainNavDestinations(
        "settings", R.string.settings, Icons.Outlined.Settings, Icons.Filled.Settings
    )
}