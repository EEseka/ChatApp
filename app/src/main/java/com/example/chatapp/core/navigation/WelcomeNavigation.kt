package com.example.chatapp.core.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatapp.R
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.authentication.presentation.signin.EmailVerificationScreen
import com.example.chatapp.authentication.presentation.signin.ResetPasswordEmailSentScreen
import com.example.chatapp.authentication.presentation.signin.SignInEvents
import com.example.chatapp.authentication.presentation.signin.SignInScreen
import com.example.chatapp.authentication.presentation.signin.SignInViewModel
import com.example.chatapp.authentication.presentation.signup.EmailVerificationScreen
import com.example.chatapp.authentication.presentation.signup.ProfileSetupScreen
import com.example.chatapp.authentication.presentation.signup.SignUpEvents
import com.example.chatapp.authentication.presentation.signup.SignUpScreen
import com.example.chatapp.authentication.presentation.signup.SignUpViewModel
import com.example.chatapp.authentication.presentation.welcome.ErrorScreen
import com.example.chatapp.authentication.presentation.welcome.SplashScreen
import com.example.chatapp.authentication.presentation.welcome.WelcomeScreen
import com.example.chatapp.authentication.presentation.welcome.WelcomeUiState
import com.example.chatapp.authentication.presentation.welcome.WelcomeViewModel
import com.example.chatapp.chat.presentation.HomeScreen
import com.example.chatapp.core.presentation.util.ObserveAsEvents
import com.example.chatapp.core.presentation.util.toString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

internal const val TAG = "WelcomeNavigation"

@Composable
fun WelcomeNavigation(
    modifier: Modifier,
    navController: NavHostController = rememberNavController(),
    welcomeViewModel: WelcomeViewModel = koinViewModel(),
    signUpViewModel: SignUpViewModel = koinViewModel(),
    signInViewModel: SignInViewModel = koinViewModel(),
    authEventBus: AuthEventBus = koinInject()
) {
    val context = LocalContext.current
    val welcomeState by welcomeViewModel.uiState.collectAsStateWithLifecycle()
    val signUpState by signUpViewModel.state.collectAsStateWithLifecycle()
    val signInState by signInViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(events = authEventBus.events) { event ->
        when (event) {
            is AuthEvent.Error -> {
                Toast.makeText(
                    context,
                    event.error.toString(context),
                    Toast.LENGTH_LONG
                ).show()
            }

            AuthEvent.SignUpSuccess -> {
                navController.navigate(
                    NavDestinations.EmailVerification.createRoute(NavDestinations.SignUp.route)
                )
            }

            AuthEvent.SignInSuccess -> {
                val isEmailVerified = signInState.isEmailVerified == true
                if (isEmailVerified) {
                    navigateAndClear(navController, NavDestinations.Home.route)
                } else {
                    signInViewModel.onEvent(SignInEvents.OnResendVerificationEmailClicked)
                    navController.navigate(
                        NavDestinations.EmailVerification.createRoute(NavDestinations.SignIn.route)
                    )
                }
            }

            AuthEvent.EmailVerified -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.email_verified_successfully),
                    Toast.LENGTH_SHORT
                ).show()
            }

            AuthEvent.ProfileSetupComplete -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.profile_setup_complete),
                    Toast.LENGTH_SHORT
                ).show()
                navigateAndClear(navController, NavDestinations.Home.route)
            }
        }
    }

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = NavDestinations.Splash.route
    ) {
        composable(NavDestinations.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    when (welcomeState) {
                        WelcomeUiState.Onboarding -> {
                            navController.navigate(NavDestinations.Welcome.route) {
                                popUpTo(NavDestinations.Splash.route) { inclusive = true }
                            }
                        }

                        WelcomeUiState.Authenticated -> {
                            navigateAndClear(navController, NavDestinations.Home.route)
                        }

                        WelcomeUiState.NotAuthenticated -> {
                            navController.navigate(NavDestinations.SignIn.route) {
                                popUpTo(NavDestinations.Splash.route) { inclusive = true }
                            }
                        }

                        WelcomeUiState.Initial -> {
                            // This state should be rare, so we add some logging just in case
                            Log.w(TAG, "Unexpected Initial state in Splash screen")
                        }

                        WelcomeUiState.Error -> {
                            navController.navigate(NavDestinations.Error.route) {
                                popUpTo(NavDestinations.Splash.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(NavDestinations.Welcome.route) {
            WelcomeScreen(
                onSignInClicked = {
                    welcomeViewModel.completeOnboarding()
                    navController.navigate(NavDestinations.SignIn.route)
                },
                onSignUpClicked = {
                    welcomeViewModel.completeOnboarding()
                    navController.navigate(NavDestinations.SignUp.route)
                }
            )
        }

        composable(NavDestinations.SignUp.route) {
            SignUpScreen(
                state = signUpState,
                onEmailValueChange = {
                    signUpViewModel.onEvent(SignUpEvents.OnEmailChanged(it))
                },
                onPasswordValueChange = {
                    signUpViewModel.onEvent(SignUpEvents.OnPasswordChanged(it))
                },
                onRepeatedPasswordValueChange = {
                    signUpViewModel.onEvent(SignUpEvents.OnRepeatedPasswordChanged(it))
                },
                onSignUpClicked = {
                    signUpViewModel.onEvent(SignUpEvents.OnSignUpClicked)
                },
                onNavigateToSignIn = {
                    navController.navigate(NavDestinations.SignIn.route) {
                        popUpTo(NavDestinations.SignUp.route) { inclusive = true }
                    }
                },
                onContinueWithGoogleClicked = {
                    signInViewModel.onEvent(SignInEvents.OnSignInWithGoogleClicked(it))
                }
            )
        }

        composable(NavDestinations.EmailVerification.route) { backStackEntry ->
            val screen = backStackEntry.arguments?.getString("screen")

            if (screen!! == NavDestinations.SignUp.route) {
                EmailVerificationScreen(
                    state = signUpState,
                    clearEmailVerificationError = {
                        signUpViewModel.onEvent(SignUpEvents.ClearEmailVerificationError)
                    },
                    onCheckVerificationClicked = {
                        signUpViewModel.onEvent(SignUpEvents.OnEmailVerifiedClicked)
                    },
                    onVerificationComplete = {
                        navController.navigate(NavDestinations.ProfileSetup.route) {
                            popUpTo(NavDestinations.EmailVerification.route) { inclusive = true }
                        }
                    }
                )
            } else if (screen == NavDestinations.SignIn.route) {
                EmailVerificationScreen(
                    state = signInState,
                    clearEmailVerificationError = {
                        signInViewModel.onEvent(SignInEvents.ClearEmailVerificationError)
                    },
                    onCheckVerificationClicked = {
                        signInViewModel.onEvent(SignInEvents.OnEmailVerifiedClicked)
                    },
                    onVerificationComplete = {
                        navigateAndClear(navController, NavDestinations.Home.route)
                    }
                )
            }
        }

        composable(NavDestinations.Error.route) {
            ErrorScreen(
                onRetryClicked = {
                    welcomeViewModel.resetAppState()
                    navigateAndClear(navController, NavDestinations.Splash.route)
                }
            )
        }

        composable(NavDestinations.SignIn.route) {
            SignInScreen(
                state = signInState,
                onEmailValueChange = {
                    signInViewModel.onEvent(SignInEvents.OnEmailChanged(it))
                },
                onPasswordValueChange = {
                    signInViewModel.onEvent(SignInEvents.OnPasswordChanged(it))
                },
                onSignInClicked = {
                    signInViewModel.onEvent(SignInEvents.OnSignInClicked)
                },
                onNavigateToSignUp = {
                    navController.navigate(NavDestinations.SignUp.route) {
                        popUpTo(NavDestinations.SignIn.route) { inclusive = true }
                    }
                },
                onForgotPasswordEmailValueChange = {
                    signInViewModel.onEvent(SignInEvents.OnForgotPasswordEmailChanged(it))
                },
                onForgotPasswordClicked = {
                    signInViewModel.onEvent(SignInEvents.OnSendPasswordResetClicked)
                },
                onDismissForgotPasswordDialog = {
                    signInViewModel.onEvent(SignInEvents.ClearForgotPasswordError)
                },
                onNavigateToResetPasswordEmail = {
                    navController.navigate(NavDestinations.ResetPasswordEmail.route)
                    signInViewModel.onEvent(SignInEvents.ClearForgotPasswordEmailSent)
                },
                onContinueWithGoogleClicked = {
                    signInViewModel.onEvent(SignInEvents.OnSignInWithGoogleClicked(it))
                }
            )
        }

        composable(NavDestinations.ResetPasswordEmail.route) {
            ResetPasswordEmailSentScreen(
                email = signInState.forgotPasswordEmail,
                onBackToSignIn = {
                    navController.navigate(NavDestinations.SignIn.route) {
                        popUpTo(NavDestinations.ResetPasswordEmail.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavDestinations.ProfileSetup.route) {
            ProfileSetupScreen(
                state = signUpState,
                onDisplayNameChanged = {
                    signUpViewModel.onEvent(SignUpEvents.OnDisplayNameChanged(it))
                },
                onPhotoSelected = { uri, extension ->
                    signUpViewModel.onEvent(SignUpEvents.OnPhotoSelected(uri, extension))
                },
                onSaveProfileClicked = {
                    signUpViewModel.onEvent(SignUpEvents.OnSaveProfileClicked)
                }
            )
        }

        composable(NavDestinations.Home.route) {
            HomeScreen(firebaseUser = null) //this should have its own viewModel later so don't forget
        }
    }
}

fun navigateAndClear(navHostController: NavHostController, route: String) {
    navHostController.navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

// TODO: reset password screen has an issue