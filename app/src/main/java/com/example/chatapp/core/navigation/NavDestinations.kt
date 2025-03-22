package com.example.chatapp.core.navigation

sealed class NavDestinations(val route: String) {
    data object Splash : NavDestinations("splash")
    data object Welcome : NavDestinations("welcome")
    data object SignIn : NavDestinations("sign_in")
    data object SignUp : NavDestinations("sign_up")
    data object EmailVerification : NavDestinations("email_verification/{screen}") {
        fun createRoute(screen: String) = "email_verification/$screen"
    }
    data object ResetPasswordEmail : NavDestinations("resetPasswordEmail")
    data object ProfileSetup : NavDestinations("profile_setup")
    data object Home : NavDestinations("home")
    data object Error : NavDestinations("error")
}