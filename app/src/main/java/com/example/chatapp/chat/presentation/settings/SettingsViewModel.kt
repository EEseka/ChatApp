package com.example.chatapp.chat.presentation.settings

import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.chat.domain.UserRepoUseCase
import com.example.chatapp.chat.presentation.MainEvent
import com.example.chatapp.chat.presentation.MainEventBus
import com.example.chatapp.core.domain.FileManager
import com.example.chatapp.core.domain.ImageCompressor
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.example.chatapp.core.domain.validation.ValidateDisplayName
import com.example.chatapp.core.presentation.util.toStringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepoUseCase: UserRepoUseCase,
    private val imageCompressor: ImageCompressor,
    private val fileManager: FileManager,
    private val validateDisplayName: ValidateDisplayName,
    private val mainEventBus: MainEventBus
) : ViewModel() {

    val user = userRepoUseCase.currentUser
    private val _state = MutableStateFlow(
        SettingsState(
            displayName = TextFieldValue(
                text = user?.displayName ?: "User${user?.uid}",
                selection = TextRange(
                    user?.displayName?.length ?: (4 + user?.uid.toString().length)
                )
            )
        )
    )
    val state = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        SettingsState()
    )

    fun onEvent(event: SettingsEvents) {
        when (event) {
            SettingsEvents.OnDeleteAccountClicked -> deleteAccount()
            is SettingsEvents.OnDisplayNameChanged -> {
                _state.update { it.copy(displayName = event.name) }
            }

            is SettingsEvents.OnPhotoSelected -> selectImage(event.uri, event.extension)
            SettingsEvents.OnSignOutClicked -> signOut()
            SettingsEvents.OnUpdateProfileClicked -> validateAndUpdateProfile()
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
                mainEventBus.send(MainEvent.Error(error))
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
                    mainEventBus.send(MainEvent.Error(error))
                }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isDeletingAccount = true) }
            userRepoUseCase.deleteAccount()
                .onSuccess {
                    _state.update { it.copy(isDeletingAccount = false) }
                    mainEventBus.send(MainEvent.AccountDeletionComplete)
                }
                .onError { error ->
                    _state.update { it.copy(isDeletingAccount = false) }
                    mainEventBus.send(MainEvent.Error(error))
                }
        }
    }

    private companion object {
        private const val MAX_IMAGE_SIZE = 256 * 1024L // 256KB
        private const val PHOTO_FILE_NAME = "compressed_profile_photo"
    }
}