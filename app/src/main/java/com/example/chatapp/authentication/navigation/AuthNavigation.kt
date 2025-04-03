package com.example.chatapp.authentication.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.chatapp.authentication.presentation.welcome.WelcomeScreen
import com.example.chatapp.authentication.presentation.welcome.WelcomeUiState
import com.example.chatapp.authentication.presentation.welcome.WelcomeViewModel
import com.example.chatapp.core.navigation.util.navigateAndClear
import com.example.chatapp.core.presentation.util.ObserveAsEvents
import com.example.chatapp.core.presentation.util.toString
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val TAG = "AuthNavigation"

@Composable
fun AuthNavigation(
    modifier: Modifier = Modifier,
    welcomeState: WelcomeUiState,
    welcomeViewModel: WelcomeViewModel,
    navController: NavHostController = rememberNavController(),
    signUpViewModel: SignUpViewModel = koinViewModel(),
    signInViewModel: SignInViewModel = koinViewModel(),
    authEventBus: AuthEventBus = koinInject(),
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val signUpState by signUpViewModel.state.collectAsStateWithLifecycle()
    val signInState by signInViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(events = authEventBus.events) { event ->
        when (event) {
            is AuthEvent.Error -> {
                Toast.makeText(
                    context,
                    event.error.toString(context),
                    Toast.LENGTH_SHORT
                ).show()
            }

            AuthEvent.SignUpSuccess -> {
                navController.navigate(
                    AuthNavDestinations.EmailVerification.createRoute(AuthNavDestinations.SignUp.route)
                )
            }

            AuthEvent.SignInSuccess -> {
                val isEmailVerified = signInState.isEmailVerified
                if (isEmailVerified) {
                    onNavigateToHome()
                } else {
                    signInViewModel.onEvent(SignInEvents.OnResendVerificationEmailClicked)
                    navController.navigate(
                        AuthNavDestinations.EmailVerification.createRoute(AuthNavDestinations.SignIn.route)
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
                onNavigateToHome()
            }
        }
    }

    val startDestination = rememberSaveable {
        when (welcomeState) {
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
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AuthNavDestinations.Welcome.route) {
            WelcomeScreen(
                onSignInClicked = {
                    welcomeViewModel.completeOnboarding()
                    navController.navigate(AuthNavDestinations.SignIn.route)
                },
                onSignUpClicked = {
                    welcomeViewModel.completeOnboarding()
                    navController.navigate(AuthNavDestinations.SignUp.route)
                }
            )
        }

        composable(AuthNavDestinations.SignUp.route) {
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
                    navController.navigate(AuthNavDestinations.SignIn.route) {
                        popUpTo(AuthNavDestinations.SignUp.route) { inclusive = true }
                    }
                },
                onContinueWithGoogleClicked = {
                    signUpViewModel.onEvent(SignUpEvents.OnSignInWithGoogleClicked)
                },
                onSetActivityContext = {
                    signUpViewModel.onEvent(SignUpEvents.OnSetActivityContext(it))
                },
                onClearActivityContext = {
                    signUpViewModel.onEvent(SignUpEvents.OnClearActivityContext)
                }
            )
        }

        composable(AuthNavDestinations.EmailVerification.route) { backStackEntry ->
            val screen = backStackEntry.arguments?.getString("screen")!!

            if (screen == AuthNavDestinations.SignUp.route) {
                EmailVerificationScreen(
                    state = signUpState,
                    clearEmailVerificationError = {
                        signUpViewModel.onEvent(SignUpEvents.ClearEmailVerificationError)
                    },
                    onCheckVerificationClicked = {
                        signUpViewModel.onEvent(SignUpEvents.OnEmailVerifiedClicked)
                    },
                    onVerificationComplete = {
                        navController.navigate(AuthNavDestinations.ProfileSetup.route) {
                            popUpTo(AuthNavDestinations.EmailVerification.route) {
                                inclusive = true
                            }
                        }
                    }
                )
            } else if (screen == AuthNavDestinations.SignIn.route) {
                EmailVerificationScreen(
                    state = signInState,
                    clearEmailVerificationError = {
                        signInViewModel.onEvent(SignInEvents.ClearEmailVerificationError)
                    },
                    onCheckVerificationClicked = {
                        signInViewModel.onEvent(SignInEvents.OnEmailVerifiedClicked)
                    },
                    onVerificationComplete = {
                        onNavigateToHome()
                    }
                )
            }
        }

        composable(AuthNavDestinations.Error.route) {
            ErrorScreen(
                onRetryClicked = {
                    welcomeViewModel.resetAppState()
                    navigateAndClear(navController, AuthNavDestinations.Welcome.route)
                }
            )
        }

        composable(AuthNavDestinations.SignIn.route) {
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
                    navController.navigate(AuthNavDestinations.SignUp.route) {
                        popUpTo(AuthNavDestinations.SignIn.route) { inclusive = true }
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
                    navController.navigate(AuthNavDestinations.ResetPasswordEmail.route)
                    signInViewModel.onEvent(SignInEvents.ClearForgotPasswordEmailSent)
                },
                onContinueWithGoogleClicked = {
                    signInViewModel.onEvent(SignInEvents.OnSignInWithGoogleClicked)
                },
                onAutomaticSignInInitiated = {
                    signInViewModel.onEvent(SignInEvents.OnAutomaticSignInInitiated)
                },
                onSetActivityContext = {
                    signInViewModel.onEvent(SignInEvents.OnSetActivityContext(it))
                },
                onClearActivityContext = {
                    signInViewModel.onEvent(SignInEvents.OnClearActivityContext)
                }
            )
        }

        composable(AuthNavDestinations.ResetPasswordEmail.route) {
            ResetPasswordEmailSentScreen(
                email = signInState.forgotPasswordEmail,
                onBackToSignIn = {
                    navController.navigate(AuthNavDestinations.SignIn.route) {
                        popUpTo(AuthNavDestinations.ResetPasswordEmail.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AuthNavDestinations.ProfileSetup.route) {
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
    }
}