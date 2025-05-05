package com.example.chatapp.chat.presentation.settings

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.R
import com.example.chatapp.chat.domain.UserRepoUseCase
import com.example.chatapp.chat.presentation.MainEvent
import com.example.chatapp.chat.presentation.MainEventBus
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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SettingsViewModel(
    private val userRepoUseCase: UserRepoUseCase,
    private val imageCompressor: ImageCompressor,
    private val fileManager: FileManager,
    private val validateDisplayName: ValidateDisplayName,
    private val mainEventBus: MainEventBus
) : ViewModel() {
    private val user = userRepoUseCase.currentUser

    // State preservation variables
    private var lastEditTimestamp: Long = 0
    private var temporaryDisplayName: TextFieldValue? = null
    private var temporaryPhotoUri: Uri? = null

    private val _state = MutableStateFlow(
        SettingsState(
            displayName = TextFieldValue(
                text = user?.displayName ?: "User${user?.uid}",
                selection = TextRange(
                    user?.displayName?.length ?: (4 + user?.uid.toString().length)
                )
            ),
            email = user?.email ?: "",
            photoUri = user?.photoUrl
        )
    )
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SettingsState(
            displayName = TextFieldValue(
                text = user?.displayName ?: "User${user?.uid}",
                selection = TextRange(
                    user?.displayName?.length ?: (4 + user?.uid.toString().length)
                )
            ),
            email = user?.email ?: "",
            photoUri = user?.photoUrl
        )
    )
    private var activityContextRef: WeakReference<Activity>? = null
    private var credentialManager: CredentialManager? = null

    fun onEvent(event: SettingsEvents) {
        when (event) {
            is SettingsEvents.OnDeleteAccountClicked -> deleteAccount()
            is SettingsEvents.OnDisplayNameChanged -> {
                lastEditTimestamp = System.currentTimeMillis()
                temporaryDisplayName = event.name
                _state.update { it.copy(displayName = event.name) }
            }

            is SettingsEvents.OnPhotoSelected -> selectImage(event.uri, event.extension)
            SettingsEvents.OnSignOutClicked -> signOut()
            SettingsEvents.OnUpdateProfileClicked -> validateAndUpdateProfile()
            SettingsEvents.OnScreenLeave -> preserveTemporaryState()
            SettingsEvents.OnScreenReturn -> restoreTemporaryState()
            SettingsEvents.OnClearActivityContext -> clearActivityContext()
            is SettingsEvents.OnSetActivityContext -> setActivityContext(event.activityContext)
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

    private fun preserveTemporaryState() {
        viewModelScope.launch {
            // Start a timer to potentially reset state
            delay(30_000)

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastEditTimestamp >= 30_000) {
                // Reset temporary state if 30 seconds have passed
                temporaryDisplayName = null
                temporaryPhotoUri = null
            }
        }
    }

    private fun restoreTemporaryState() {
        val currentTime = System.currentTimeMillis()

        // Restore temporary display name if within 30 seconds
        if (temporaryDisplayName != null &&
            currentTime - lastEditTimestamp < 30_000
        ) {
            _state.update {
                it.copy(
                    displayName = temporaryDisplayName!!,
                    photoUri = temporaryPhotoUri ?: it.photoUri
                )
            }
        } else {
            // Reset to original state if 30 seconds have passed
            _state.update {
                it.copy(
                    displayName = TextFieldValue(
                        text = user?.displayName ?: "User${user?.uid}",
                        selection = TextRange(
                            user?.displayName?.length ?: (4 + user?.uid.toString().length)
                        )
                    ),
                    photoUri = user?.photoUrl
                )
            }
            temporaryDisplayName = null
            temporaryPhotoUri = null
        }
    }

    private fun selectImage(uri: Uri, extension: String) {
        viewModelScope.launch {
            _state.update { it.copy(isProfileUpdating = true) }

            imageCompressor.compressImage(uri, MAX_IMAGE_SIZE)
                .onSuccess { byteArray ->
                    val compressedUri = fileManager.saveImageToCache(
                        byteArray,
                        "$PHOTO_FILE_NAME${System.currentTimeMillis()}.$extension"
                    )

                    lastEditTimestamp = System.currentTimeMillis()
                    temporaryPhotoUri = compressedUri

                    _state.update {
                        it.copy(
                            photoUri = compressedUri,
                            isProfileUpdating = false
                        )
                    }
                }
                .onError { error ->
                    _state.update {
                        it.copy(
                            isProfileUpdating = false,
                            photoUriError = error.toStringRes()
                        )
                    }
                }
        }
    }

    private fun validateAndUpdateProfile() {
        val displayNameResult = validateDisplayName(_state.value.displayName.text.trim())

        if (!displayNameResult.successful) {
            _state.update {
                it.copy(displayNameError = displayNameResult.errorMessage?.toStringRes())
            }
            return
        }
        _state.update { it.copy(displayNameError = null) }
        viewModelScope.launch {
            _state.update { it.copy(isProfileUpdating = true) }

            userRepoUseCase.updateProfile(
                displayName = if (_state.value.displayName.text.trim() == user?.displayName) null else _state.value.displayName.text.trim(),
                photoUri = if (_state.value.photoUri == user?.photoUrl) null else _state.value.photoUri
            ).onSuccess {
                _state.update { it.copy(isProfileUpdating = false) }
                mainEventBus.send(MainEvent.ProfileUpdateComplete)
            }.onError { error ->
                _state.update { it.copy(isProfileUpdating = false) }
                mainEventBus.send(MainEvent.AuthError(error))
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isSigningOut = true) }

            userRepoUseCase.signOut()
                .onSuccess {
                    _state.update { it.copy(isSigningOut = false) }
                    mainEventBus.send(MainEvent.SignOutComplete)
                }
                .onError { error ->
                    _state.update { it.copy(isSigningOut = false) }
                    mainEventBus.send(MainEvent.AuthError(error))
                }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true) }

            if (user == null) {
                _state.update { it.copy(isDeletingAccount = false) }
                mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.USER_NOT_FOUND))
                return@launch
            }

            try {
                // Get actual provider data
                val providerData = user.providerData

                // Check available providers (look for email or Google)
                val hasEmailProvider =
                    providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
                val hasGoogleProvider =
                    providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

                if (hasEmailProvider || hasGoogleProvider) {
                    reAuthenticateAndDeleteAccount()
                } else {
                    _state.update { it.copy(isDeletingAccount = false) }
                    mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.UNKNOWN))
                    Log.e(
                        TAG,
                        "Unsupported provider. Available providers: ${providerData.map { it.providerId }}"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isDeletingAccount = false) }
                mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.FAILED_REAUTHENTICATION))
                Log.e(TAG, "Failed to re-authenticate user", e)
            }
        }
    }

    private fun reAuthenticateAndDeleteAccount() {
        viewModelScope.launch {
            try {
                val context = activityContextRef?.get() ?: return@launch
                // Get available providers
                val providerData = user?.providerData ?: emptyList()
                val hasEmailProvider =
                    providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
                val hasGoogleProvider =
                    providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

                // Set up request with appropriate options
                val request = GetCredentialRequest.Builder()

                // Add email option if available
                if (hasEmailProvider) {
                    request.addCredentialOption(GetPasswordOption())
                }

                // Add Google option if available
                if (hasGoogleProvider) {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .build()
                    request.addCredentialOption(googleIdOption)
                }

                val credentialResponse = credentialManager?.getCredential(
                    context = context,
                    request = request.build()
                ) ?: return@launch

                when (val credential = credentialResponse.credential) {
                    is PasswordCredential -> {
                        val email = user?.email ?: return@launch
                        val authCredential = EmailAuthProvider.getCredential(
                            email,
                            credential.password
                        )
                        completeReauthenticationAndDeletion(authCredential)
                    }

                    is CustomCredential -> {
                        if (credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            val authCredential = GoogleAuthProvider.getCredential(
                                googleIdTokenCredential.idToken,
                                null
                            )
                            completeReauthenticationAndDeletion(authCredential)
                        } else {
                            _state.update { it.copy(isDeletingAccount = false) }
                            mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.FAILED_REAUTHENTICATION))
                            Log.e(TAG, "Unknown credential type: ${credential.type}")
                        }
                    }

                    else -> {
                        _state.update { it.copy(isDeletingAccount = false) }
                        mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.FAILED_REAUTHENTICATION))
                        Log.e(TAG, "Unsupported credential type")
                    }
                }
            } catch (e: GetCredentialCancellationException) {
                _state.update { it.copy(isDeletingAccount = false) }
                Log.d(TAG, "User cancelled the re-authentication request", e)
            } catch (e: NoCredentialException) {
                // User did not save to credential Manager but we can still delete account provided they recently signed in
                Log.d(TAG, "No credential found", e)
                deleteAccountWithoutReAuth()
            } catch (e: GetCredentialException) {
                // User does not have any credential set up on his device yet but we can still delete account provided they recently signed in
                Log.e(TAG, "AuthError getting credential", e)
                deleteAccountWithoutReAuth()
            } catch (e: Exception) {
                _state.update { it.copy(isDeletingAccount = false) }
                Log.e(TAG, "Re-authentication error", e)
                mainEventBus.send(MainEvent.AuthError(FirebaseAuthError.FAILED_REAUTHENTICATION))
            }
        }
    }

    private suspend fun completeReauthenticationAndDeletion(authCredential: AuthCredential) {
        userRepoUseCase.reAuthenticateUser(authCredential)
            .onSuccess {
                userRepoUseCase.deleteAccount()
                    .onSuccess {
                        try {
                            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
                                ?: return
                        } catch (e: ClearCredentialException) {
                            Log.e(TAG, "Failed to clear credential state", e)
                        }
                        _state.update { it.copy(isDeletingAccount = false) }
                        mainEventBus.send(MainEvent.AccountDeletionComplete)
                    }
                    .onError { error ->
                        _state.update { it.copy(isDeletingAccount = false) }
                        mainEventBus.send(MainEvent.AuthError(error))
                    }
            }
            .onError { error ->
                _state.update { it.copy(isDeletingAccount = false) }
                mainEventBus.send(MainEvent.AuthError(error))
            }
    }

    // Useful for users without credential manager.
    // We let firebase handle it by requiring recent sign in
    private suspend fun deleteAccountWithoutReAuth() {
        userRepoUseCase.deleteAccount()
            .onSuccess {
                _state.update { it.copy(isDeletingAccount = false) }
                mainEventBus.send(MainEvent.AccountDeletionComplete)
            }
            .onError { error ->
                _state.update { it.copy(isDeletingAccount = false) }
                mainEventBus.send(MainEvent.AuthError(error))
            }
    }

    private companion object {
        private const val TAG = "SettingsViewModel"
        private const val MAX_IMAGE_SIZE = 256 * 1024L // 256KB
        private const val PHOTO_FILE_NAME = "compressed_profile_photo"
    }
}