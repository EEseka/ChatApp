package com.example.chatapp.chat.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chatapp.R

sealed class MainNavDestinations(
    val route: String,
    @StringRes val label: Int,
    val outlinedIcon: ImageVector? = null,
    val filledIcon: ImageVector? = null
) {
    data object Home : MainNavDestinations(
        "home", R.string.home, Icons.Outlined.Home, Icons.Filled.Home
    )

    //    data object Search : MainNavDestinations("search")
    data object Settings : MainNavDestinations(
        "settings", R.string.settings, Icons.Outlined.Settings, Icons.Filled.Settings
    )
}