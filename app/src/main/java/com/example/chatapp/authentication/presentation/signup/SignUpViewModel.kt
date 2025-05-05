package com.example.chatapp.authentication.presentation.signup

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidatePassword
import com.example.chatapp.authentication.domain.validation.ValidateRepeatedPassword
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.core.domain.utils.FileManager
import com.example.chatapp.core.domain.utils.ImageCompressor
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.domain.validation.ValidateDisplayName
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

class SignUpViewModel(
    private val userAuthUseCase: UserAuthUseCase,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword,
    private val validateRepeatedPassword: ValidateRepeatedPassword,
    private val validateDisplayName: ValidateDisplayName,
    private val imageCompressor: ImageCompressor,
    private val fileManager: FileManager,
    private val authEventBus: AuthEventBus
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SignUpState()
    )
    private val isEmailVerified get() = userAuthUseCase.currentUser?.isEmailVerified == true
    private var activityContextRef: WeakReference<Activity>? = null
    private var credentialManager: CredentialManager? = null

    fun onEvent(event: SignUpEvents) {
        when (event) {
            is SignUpEvents.OnEmailChanged -> {
                _state.update { it.copy(email = event.email.trim()) }
            }

            is SignUpEvents.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password.trim()) }
            }

            is SignUpEvents.OnRepeatedPasswordChanged -> {
                _state.update { it.copy(repeatedPassword = event.repeatedPassword.trim()) }
            }

            SignUpEvents.OnSignUpClicked -> validateAndSubmit()
            SignUpEvents.OnSignInWithGoogleClicked -> signInWithGoogle()
            SignUpEvents.OnEmailVerifiedClicked -> checkEmailVerification()
            SignUpEvents.ClearEmailVerificationError -> clearEmailVerificationError()
            is SignUpEvents.OnDisplayNameChanged -> {
                _state.update { it.copy(displayName = event.name) }
            }

            is SignUpEvents.OnPhotoSelected -> selectImage(event.uri, event.extension)
            SignUpEvents.OnSaveProfileClicked -> validateAndSaveProfile()
            SignUpEvents.OnClearActivityContext -> clearActivityContext()
            is SignUpEvents.OnSetActivityContext -> setActivityContext(event.activityContext)
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

    private fun validateAndSubmit() {
        val emailResult = validateEmail(_state.value.email)
        val passwordResult = validatePassword(_state.value.password)
        val repeatedPasswordResult =
            validateRepeatedPassword(_state.value.password, _state.value.repeatedPassword)

        val hasError = listOf(
            emailResult,
            passwordResult,
            repeatedPasswordResult
        ).any { !it.successful }

        if (hasError) {
            _state.update {
                it.copy(
                    emailError = emailResult.errorMessage?.toStringRes(),
                    passwordError = passwordResult.errorMessage?.toStringRes(),
                    repeatedPasswordError = repeatedPasswordResult.errorMessage?.toStringRes()
                )
            }
            return
        }
        _state.update {
            it.copy(
                emailError = null,
                passwordError = null,
                repeatedPasswordError = null
            )
        }
        signUp()
    }

    private fun signUp(
        email: String = _state.value.email,
        password: String = _state.value.password
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            userAuthUseCase.createAccount(email, password)
                .onSuccess {
                    try {
                        val context = activityContextRef?.get() ?: return@launch
                        credentialManager?.createCredential(
                            context = context,
                            request = CreatePasswordRequest(
                                id = email,
                                password = password
                            )
                        )
                    } catch (e: CreateCredentialCancellationException) {
                        authEventBus.send(AuthEvent.Error(FirebaseAuthError.CREDENTIAL_CREATION_ERROR))
                        Log.w(TAG, "User cancelled credential creation", e)
                    } catch (e: CreateCredentialException) {
                        Log.e(TAG, "AuthError creating credential", e)
                    }
                    _state.update { it.copy(isLoading = false) }
                    sendVerificationEmail()
                    authEventBus.send(AuthEvent.SignUpSuccess)
                }
                .onError { error ->
                    _state.update { it.copy(isLoading = false) }
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    private fun sendVerificationEmail() {
        viewModelScope.launch {
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

    private fun selectImage(uri: Uri, extension: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            imageCompressor.compressImage(uri, MAX_IMAGE_SIZE)
                .onSuccess { byteArray ->
                    val compressedUri = fileManager.saveImageToCache(
                        byteArray,
                        "${PHOTO_FILE_NAME}${System.currentTimeMillis()}.$extension"
                    )
                    _state.update {
                        it.copy(
                            photoUri = compressedUri,
                            isLoading = false
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            photoUriError = error.toStringRes()
                        )
                    }
                }
        }
    }

    private fun validateAndSaveProfile() {
        val displayNameResult = validateDisplayName(_state.value.displayName.trim())

        if (!displayNameResult.successful) {
            _state.update {
                it.copy(displayNameError = displayNameResult.errorMessage?.toStringRes())
            }
            return
        }
        _state.update { it.copy(displayNameError = null) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            userAuthUseCase.updateProfile(
                displayName = _state.value.displayName.trim(),
                photoUri = _state.value.photoUri
            ).onSuccess {
                _state.update { it.copy(isLoading = false) }
                authEventBus.send(AuthEvent.ProfileSetupComplete)
            }.onError { error ->
                _state.update { it.copy(isLoading = false) }
                authEventBus.send(AuthEvent.Error(error))
            }
        }
    }

    private fun clearEmailVerificationError() =
        _state.update { it.copy(emailVerificationError = null) }

    private companion object {
        private const val TAG = "SignUpViewModel"
        private const val MAX_IMAGE_SIZE = 256 * 1024L // 256KB
        private const val PHOTO_FILE_NAME = "compressed_profile_photo"
    }
}