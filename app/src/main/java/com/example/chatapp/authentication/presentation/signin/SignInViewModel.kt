package com.example.chatapp.authentication.presentation.signin

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidateSignInPassword
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.core.domain.util.FirebaseError
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

class SignInViewModel(
    private val userAuthUseCase: UserAuthUseCase,
    private val credentialManager: CredentialManager,
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

            is SignInEvents.OnSignInWithGoogleClicked -> signInWithGoogle(context = event.context)
            SignInEvents.OnResendVerificationEmailClicked -> resendVerificationEmail()
            SignInEvents.OnSignInClicked -> validateAndSignIn()
            SignInEvents.OnEmailVerifiedClicked -> checkEmailVerification()
            SignInEvents.OnSendPasswordResetClicked -> validateAndSendPasswordReset()
            SignInEvents.ClearForgotPasswordEmailSent -> clearForgotPasswordEmailSent()
        }
    }

    private fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    userAuthUseCase.signInWithGoogle(googleIdTokenCredential.idToken)
                        .onSuccess {
                            _state.update { it.copy(isLoading = false, isEmailVerified = true) }
                            authEventBus.send(AuthEvent.SignInSuccess)
                        }
                        .onError { error ->
                            _state.update { it.copy(isLoading = false) }
                            authEventBus.send(AuthEvent.Error(error))
                        }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(FirebaseError.GOOGLE_SIGN_IN_FAILED))
                    Log.e(TAG, "Invalid credential type: ${credential.type}")
                }

            } catch (e: GetCredentialException) {
                _state.update { it.copy(isLoading = false) }
                Log.e(TAG, e.message.orEmpty())
                authEventBus.send(AuthEvent.Error(FirebaseError.UNKNOWN))
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
                    Log.w(TAG, "Error reloading user : $error")
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
        sendPasswordReset()
    }

    private fun sendPasswordReset() {
        TODO("Check here")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userAuthUseCase.sendPasswordResetEmail(_state.value.forgotPasswordEmail)
                .onSuccess {
                    // Shouldn't this be successful only if the account exists?
                    Log.d(TAG, "Password reset email sent to ${_state.value.forgotPasswordEmail}")
                    _state.update { it.copy(isLoading = false, forgotPasswordEmailSent = true) }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false, forgotPasswordEmailSent = false) }
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
        _state.update { it.copy(emailVerificationError = null, forgotPasswordEmailSent = false) }
    }

    private fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            userAuthUseCase.signOut()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            userAuthUseCase.deleteAccount()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    companion object {
        private const val TAG = "SignInViewModel"
    }
}