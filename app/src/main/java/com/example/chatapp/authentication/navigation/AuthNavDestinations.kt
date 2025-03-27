package com.example.chatapp.authentication.navigation

sealed class AuthNavDestinations(val route: String) {
    data object Welcome : AuthNavDestinations("welcome")
    data object SignIn : AuthNavDestinations("sign_in")
    data object SignUp : AuthNavDestinations("sign_up")
    data object EmailVerification : AuthNavDestinations("email_verification/{screen}") {
        fun createRoute(screen: String) = "email_verification/$screen"
    }
    data object ResetPasswordEmail : AuthNavDestinations("resetPasswordEmail")
    data object ProfileSetup : AuthNavDestinations("profile_setup")
    data object Error : AuthNavDestinations("error")
}