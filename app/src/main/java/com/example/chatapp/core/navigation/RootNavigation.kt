package com.example.chatapp.core.navigation

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatapp.authentication.navigation.AuthNavDestinations
import com.example.chatapp.authentication.navigation.AuthNavigation
import com.example.chatapp.authentication.presentation.welcome.SplashScreen
import com.example.chatapp.authentication.presentation.welcome.WelcomeUiState
import com.example.chatapp.authentication.presentation.welcome.WelcomeViewModel
import com.example.chatapp.chat.navigation.MainNavigation
import com.example.chatapp.core.navigation.util.navigateAndClear
import org.koin.androidx.compose.koinViewModel

internal const val TAG = "RootNavigation"

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RootNavigation(
    modifier: Modifier,
    navController: NavHostController = rememberNavController(),
    welcomeViewModel: WelcomeViewModel = koinViewModel()
) {
    val welcomeState by welcomeViewModel.uiState.collectAsStateWithLifecycle()

    val authNavStartDestination = when (welcomeState) {
        WelcomeUiState.Onboarding -> AuthNavDestinations.Welcome.route
        WelcomeUiState.NotAuthenticated -> AuthNavDestinations.SignIn.route
        WelcomeUiState.Initial -> {
            // This state should be rare, so we add some logging just in case
            Log.w(TAG, "Unexpected Initial state in Splash screen")
            AuthNavDestinations.Welcome.route
        }

        WelcomeUiState.Error -> AuthNavDestinations.Error.route
        WelcomeUiState.Authenticated -> {
            // Can Never be reached, but we need to handle all cases
            AuthNavDestinations.SignIn.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = RootNavDestinations.Splash.route
    ) {
        composable(RootNavDestinations.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    when (welcomeState) {
                        WelcomeUiState.Authenticated -> {
                            navigateAndClear(navController, RootNavDestinations.Main.route)
                        }

                        else -> {
                            navigateAndClear(navController, RootNavDestinations.Auth.route)
                        }
                    }
                }
            )
        }

        composable(RootNavDestinations.Auth.route) {
            AuthNavigation(
                modifier = modifier,
                startDestination = authNavStartDestination,
                welcomeViewModel = welcomeViewModel,
                onNavigateToHome = {
                    navigateAndClear(navController, RootNavDestinations.Main.route)
                }
            )
        }

        composable(RootNavDestinations.Main.route) {
            MainNavigation(
                modifier = modifier,
                onLogout = {
                    navigateAndClear(navController, RootNavDestinations.Auth.route)
                }
            )
        }
    }
}