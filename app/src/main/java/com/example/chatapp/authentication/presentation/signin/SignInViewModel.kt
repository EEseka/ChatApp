package com.example.chatapp.authentication.presentation.signin

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidateSignInPassword
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.presentation.util.toStringRes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SignInViewModel(
    private val userAuthUseCase: UserAuthUseCase,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidateSignInPassword,
    private val authEventBus: AuthEventBus
) : ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SignInState()
    )
    private val isEmailVerified
        get() = userAuthUseCase.currentUser?.isEmailVerified == true
    private var activityContextRef: WeakReference<Activity>? = null
    private var credentialManager: CredentialManager? = null

    fun onEvent(event: SignInEvents) {
        when (event) {
            SignInEvents.ClearEmailVerificationError -> clearEmailVerificationError()
            SignInEvents.ClearForgotPasswordError -> clearForgotPasswordError()
            is SignInEvents.OnEmailChanged -> {
                _state.update { it.copy(email = event.email.trim()) }
            }

            is SignInEvents.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password.trim()) }
            }

            is SignInEvents.OnForgotPasswordEmailChanged -> {
                _state.update { it.copy(forgotPasswordEmail = event.email.trim()) }
            }

            SignInEvents.OnSignInWithGoogleClicked -> signInWithGoogle()
            SignInEvents.OnResendVerificationEmailClicked -> resendVerificationEmail()
            SignInEvents.OnSignInClicked -> validateAndSignIn()
            SignInEvents.OnEmailVerifiedClicked -> checkEmailVerification()
            SignInEvents.OnSendPasswordResetClicked -> validateAndSendPasswordReset()
            SignInEvents.ClearForgotPasswordEmailSent -> clearForgotPasswordEmailSent()
            SignInEvents.OnAutomaticSignInInitiated -> automaticSignIn()
            SignInEvents.OnClearActivityContext -> clearActivityContext()
            is SignInEvents.OnSetActivityContext -> setActivityContext(event.activityContext)
        }
    }

    private fun setActivityContext(activity: Activity) {
        activityContextRef = WeakReference(activity)
        credentialManager = CredentialManager.create(activity)
    }

    private fun clearActivityContext() {
        activityContextRef?.clear()
        activityContextRef = null
        credentialManager = null
    }

    override fun onCleared() {
        super.onCleared()
        clearActivityContext()
    }

    private fun automaticSignIn() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val context = activityContextRef?.get() ?: return@launch
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(
                        context.getString(R.string.default_web_client_id)
                    )
                    .build()

                val credentialResponse = credentialManager?.getCredential(
                    context = context,
                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(GetPasswordOption())
                        .addCredentialOption(googleIdOption)
                        .build()
                ) ?: return@launch
                when (val credential = credentialResponse.credential) {
                    is PasswordCredential -> {
                        userAuthUseCase.signIn(credential.id, credential.password)
                            .onSuccess {
                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        isEmailVerified = isEmailVerified,
                                        emailVerificationError = if (!isEmailVerified) R.string.email_not_verified else null
                                    )
                                }
                                authEventBus.send(AuthEvent.SignInSuccess)
                            }
                            .onError { error ->
                                _state.update { it.copy(isLoading = false) }
                                authEventBus.send(AuthEvent.Error(error))
                            }
                    }

                    is CustomCredential -> {
                        if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            userAuthUseCase.signInWithGoogle(googleIdTokenCredential.idToken)
                                .onSuccess {
                                    _state.update {
                                        it.copy(
                                            isLoading = false,
                                            isEmailVerified = true
                                        )
                                    }
                                    authEventBus.send(AuthEvent.SignInSuccess)
                                }
                                .onError { error ->
                                    _state.update { it.copy(isLoading = false) }
                                    authEventBus.send(AuthEvent.Error(error))
                                }
                        } else {
                            _state.update { it.copy(isLoading = false) }
                            authEventBus.send(AuthEvent.Error(FirebaseAuthError.GOOGLE_SIGN_IN_FAILED))
                            Log.e(TAG, "Unknown credential type: ${credential.type}")
                        }
                    }

                    else -> {
                        _state.update { it.copy(isLoading = false) }
                        authEventBus.send(AuthEvent.Error(FirebaseAuthError.GOOGLE_SIGN_IN_FAILED))
                        Log.e(TAG, "Unsupported credential type")
                    }
                }

            } catch (e: GetCredentialCancellationException) {
                _state.update { it.copy(isLoading = false) }
                Log.d(TAG, "User cancelled the sign-in request", e)
            } catch (e: NoCredentialException) {
                _state.update { it.copy(isLoading = false) }
                Log.w(TAG, "No credential found", e)
            } catch (e: GetCredentialException) {
                _state.update { it.copy(isLoading = false) }
                Log.e(TAG, "AuthError getting credential", e)
            }
        }
    }

    private fun signInWithGoogle() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val context = activityContextRef?.get() ?: return@launch
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager?.getCredential(
                    request = request,
                    context = context
                ) ?: return@launch

                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    userAuthUseCase.signInWithGoogle(googleIdTokenCredential.idToken)
                        .onSuccess {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isEmailVerified = isEmailVerified,
                                    emailVerificationError = if (!isEmailVerified) R.string.email_not_verified else null
                                )
                            }
                            authEventBus.send(AuthEvent.SignInSuccess)
                        }
                        .onError { error ->
                            _state.update { it.copy(isLoading = false) }
                            authEventBus.send(AuthEvent.Error(error))
                        }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(FirebaseAuthError.GOOGLE_SIGN_IN_FAILED))
                    Log.e(TAG, "Invalid credential type: ${credential.type}")
                }

            } catch (e: GetCredentialCancellationException) {
                _state.update { it.copy(isLoading = false) }
                Log.d(TAG, "User cancelled the sign-in request", e)
            } catch (e: NoCredentialException) {
                _state.update { it.copy(isLoading = false) }
                Log.w(TAG, "No credential found", e)
                authEventBus.send(AuthEvent.Error(FirebaseAuthError.CREDENTIAL_NOT_FOUND))
            } catch (e: GetCredentialException) {
                _state.update { it.copy(isLoading = false) }
                Log.e(TAG, e.message.orEmpty())
                authEventBus.send(AuthEvent.Error(FirebaseAuthError.GOOGLE_SIGN_IN_FAILED))
            }

        }
    }

    private fun validateAndSignIn() {
        val emailResult = validateEmail(_state.value.email)
        val passwordResult = validatePassword(_state.value.password)

        val hasError = listOf(
            emailResult,
            passwordResult
        ).any { !it.successful }

        if (hasError) {
            _state.update {
                it.copy(
                    emailError = emailResult.errorMessage?.toStringRes(),
                    passwordError = passwordResult.errorMessage?.toStringRes()
                )
            }
            return
        }
        _state.update { it.copy(emailError = null, passwordError = null) }
        signIn()
    }

    private fun signIn() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userAuthUseCase.signIn(email = _state.value.email, password = _state.value.password)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isEmailVerified = isEmailVerified,
                            emailVerificationError = if (!isEmailVerified) R.string.email_not_verified else null
                        )
                    }
                    authEventBus.send(AuthEvent.SignInSuccess)
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    private fun checkEmailVerification() {
        viewModelScope.launch {
            userAuthUseCase.reloadUser()
                .onSuccess {
                    _state.update {
                        it.copy(
                            isEmailVerified = isEmailVerified,
                            emailVerificationError = if (!isEmailVerified) R.string.email_not_verified else null
                        )
                    }
                    if (isEmailVerified) {
                        authEventBus.send(AuthEvent.EmailVerified)
                    }
                }
                .onError { error ->
                    Log.w(TAG, "AuthError reloading user : $error")
                }
        }
    }


    private fun resendVerificationEmail() {
        viewModelScope.launch {
            if (isEmailVerified) {
                Log.d(TAG, "User already verified. No need to resend email.")
                return@launch
            }
            _state.update { it.copy(isLoading = true) }

            userAuthUseCase.sendEmailVerification()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    private fun validateAndSendPasswordReset() {
        val emailResult = validateEmail(_state.value.forgotPasswordEmail)
        if (!emailResult.successful) {
            _state.update {
                it.copy(forgotPasswordEmailError = emailResult.errorMessage?.toStringRes())
            }
            return
        }
        _state.update { it.copy(forgotPasswordEmailError = null) }
        sendPasswordReset()
    }

    private fun sendPasswordReset() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userAuthUseCase.sendPasswordResetEmail(_state.value.forgotPasswordEmail)
                .onSuccess {
                    // Shouldn't this be successful only if the account exists?
                    Log.d(
                        TAG,
                        "Password reset email sent to ${_state.value.forgotPasswordEmail}"
                    )
                    _state.update { it.copy(isLoading = false, forgotPasswordEmailSent = true) }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            forgotPasswordEmailSent = false
                        )
                    }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    private fun clearEmailVerificationError() {
        _state.update { it.copy(emailVerificationError = null) }
    }

    private fun clearForgotPasswordEmailSent() {
        _state.update { it.copy(forgotPasswordEmailSent = false) }
    }

    private fun clearForgotPasswordError() {
        _state.update {
            it.copy(
                emailVerificationError = null,
                forgotPasswordEmailSent = false
            )
        }
    }

    companion object {
        private const val TAG = "SignInViewModel"
    }
}