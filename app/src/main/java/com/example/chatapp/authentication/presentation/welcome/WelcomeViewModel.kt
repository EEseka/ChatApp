package com.example.chatapp.authentication.presentation.welcome

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatapp.authentication.domain.CheckFirstInstallUseCase
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.authentication.presentation.AuthEvent
import com.example.chatapp.authentication.presentation.AuthEventBus
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val checkFirstInstallUseCase: CheckFirstInstallUseCase,
    private val userAuthUseCase: UserAuthUseCase,
    private val authEventBus: AuthEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Initial)
    val uiState = _uiState.asStateFlow()

    private val isEmailVerified
        get() = userAuthUseCase.currentUser?.isEmailVerified == true

    init {
        observeAuthState()
        observeFirstInstallState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            userAuthUseCase.getAuthState()
                .onSuccess { flow ->
                    flow.collect { isUserAuthenticated ->
                        val newState = when {
                            isUserAuthenticated && isEmailVerified -> WelcomeUiState.Authenticated
                            else -> WelcomeUiState.NotAuthenticated
                        }
                        _uiState.update { newState }
                    }
                }
                .onError { error ->
                    authEventBus.send(AuthEvent.Error(error))
                }
        }
    }

    // we don't use runCatching here because we handled the error in the firstInstallImpl with flow.catch
    private fun observeFirstInstallState() {
        viewModelScope.launch {
            checkFirstInstallUseCase().collect { isFirstInstall ->
                if (isFirstInstall) {
                    _uiState.update { WelcomeUiState.Onboarding }
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            runCatching {
                checkFirstInstallUseCase.markOnboardingComplete()
                _uiState.update { WelcomeUiState.NotAuthenticated }
            }.onFailure { exception ->
                handleUnexpectedException(exception)
            }
        }
    }

    private fun handleUnexpectedException(exception: Throwable) {
        Log.e(TAG, "Unexpected error in authentication flow", exception)
        viewModelScope.launch {
            _uiState.update { WelcomeUiState.Error }
            authEventBus.send(AuthEvent.Error(FirebaseAuthError.UNKNOWN))
        }
    }

    // Optional method to reset app state (useful for testing)
    // This won't be needed in production though
    fun resetAppState() {
        viewModelScope.launch {
            checkFirstInstallUseCase.resetFirstLaunchState()
            observeFirstInstallState()
        }
    }

    companion object {
        private const val TAG = "WelcomeViewModel"
    }
}