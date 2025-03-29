package com.example.chatapp.authentication.presentation.signup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.domain.validation.ValidateEmail
import com.example.chatapp.authentication.domain.validation.ValidatePassword
import com.example.chatapp.authentication.domain.validation.ValidateRepeatedPassword
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.core.domain.FileManager
import com.example.chatapp.core.domain.ImageCompressor
import com.example.chatapp.core.domain.util.FirebaseError
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.domain.validation.ValidateDisplayName
import com.example.chatapp.core.presentation.util.toStringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val userAuthUseCase: UserAuthUseCase,
    private val validateEmail: ValidateEmail,
    private val validatePassword: ValidatePassword,
    private val validateRepeatedPassword: ValidateRepeatedPassword,
    private val validateDisplayName: ValidateDisplayName,
    private val imageCompressor: ImageCompressor,
    private val fileManager: FileManager,
    private val authEventBus: AuthEventBus,
    private val appContext: Context
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpState())
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SignUpState()
    )
    private val isEmailVerified get() = userAuthUseCase.currentUser?.isEmailVerified == true
    private val credentialManager = CredentialManager.create(appContext)

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
            SignUpEvents.OnEmailVerifiedClicked -> checkEmailVerification()
            SignUpEvents.ClearEmailVerificationError -> clearEmailVerificationError()
            is SignUpEvents.OnDisplayNameChanged -> {
                _state.update { it.copy(displayName = event.name) }
            }

            is SignUpEvents.OnPhotoSelected -> selectImage(event.uri, event.extension)
            SignUpEvents.OnSaveProfileClicked -> validateAndSaveProfile()
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
                        credentialManager.createCredential(
                            context = appContext,
                            request = CreatePasswordRequest(
                                id = email,
                                password = password
                            )
                        )
                    } catch (e: CreateCredentialCancellationException) {
                        authEventBus.send(AuthEvent.Error(FirebaseError.CREDENTIAL_CREATION_ERROR))
                        Log.w(TAG, "User cancelled credential creation", e)
                    } catch (e: CreateCredentialException) {
                        authEventBus.send(AuthEvent.Error(FirebaseError.CREDENTIAL_CREATION_ERROR))
                        Log.e(TAG, "Error creating credential", e)
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
                    Log.w(TAG, "Error reloading user : $error")
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