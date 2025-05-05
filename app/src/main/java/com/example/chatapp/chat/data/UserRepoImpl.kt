package com.example.chatapp.chat.data

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.chatapp.chat.domain.UserRepoUseCase
import com.example.chatapp.core.data.firebase.utils.safeFirebaseAuthCall
import com.example.chatapp.core.data.firebase.utils.safeFirebaseStorageCall
import com.example.chatapp.core.domain.MediaStorage
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.FirebaseStorageError
import com.example.chatapp.core.domain.util.Result
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class UserRepoImpl(
    private val auth: FirebaseAuth,
    private val storage: MediaStorage
) : UserRepoUseCase {
    // StateFlow for observing the current user state
    private val _currentUserFlow: StateFlow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user)

            // This is added to enable the app detect account deletion apart from sign out and sign in
            user?.reload()?.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    trySend(null) // Force emit null if the user is no longer valid
                }
            }
        }

        auth.addAuthStateListener(authStateListener)
        // Ensure we emit the current user immediately
        // For example when there is a new collector
        trySend(auth.currentUser)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        started = SharingStarted.Eagerly,
        initialValue = auth.currentUser
    )

    override val currentUser: FirebaseUser?
        get() = _currentUserFlow.value

    override suspend fun signOut(): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            auth.signOut()
        }

    override suspend fun deleteAccount(): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            currentUser?.delete()?.await()
        }

    override suspend fun updateProfile(
        displayName: String?,
        photoUri: Uri?
    ): Result<Unit, FirebaseAuthError> {
        val userId = currentUser?.uid
        var finalPhotoUrl: String? = null

        if (photoUri != null && userId != null) {
            safeFirebaseStorageCall {
                storage.uploadPicture(PROFILE_PHOTO_STORAGE_PATH + userId, photoUri)
                    .onSuccess { secureUrl ->
                        finalPhotoUrl = secureUrl
                    }
                    .onError { error ->
                        Log.e(TAG, "Failed to upload profile image to Firebase Storage : $error")
                        Result.Error(FirebaseStorageError.IO_ERROR)
                    }
            }
        }


        return safeFirebaseAuthCall {
            val profileUpdates = userProfileChangeRequest {
                displayName?.let { this.displayName = it }
                finalPhotoUrl?.let { this.photoUri = it.toUri() }
            }

            currentUser?.updateProfile(profileUpdates)?.await()
            currentUser?.reload()?.await()
        }
    }

    override suspend fun reAuthenticateUser(credential: AuthCredential): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            currentUser?.reauthenticate(credential)?.await()
        }

    companion object {
        private const val PROFILE_PHOTO_STORAGE_PATH = "profile_pictures/"
        private const val TAG = "UserRepoImpl"
    }
}