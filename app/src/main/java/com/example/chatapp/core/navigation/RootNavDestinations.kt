package com.example.chatapp.core.navigation

sealed class RootNavDestinations(val route: String) {
    data object Splash : RootNavDestinations("splash")
    data object Auth : RootNavDestinations("auth")
    data object Main : RootNavDestinations("main")
}