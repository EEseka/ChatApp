package com.example.chatapp.authentication.data

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.core.data.firebase.utils.safeFirebaseAuthCall
import com.example.chatapp.core.data.firebase.utils.safeFirebaseStorageCall
import com.example.chatapp.core.domain.MediaStorage
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.FirebaseStorageError
import com.example.chatapp.core.domain.util.Result
import com.example.chatapp.core.domain.util.onError
import com.example.chatapp.core.domain.util.onSuccess
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val storage: MediaStorage
) : UserAuthUseCase {
    // StateFlow for observing the current user state
    private val _currentUserFlow: StateFlow<FirebaseUser?> = callbackFlow {
        val authStateListener = AuthStateListener { auth ->
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

    override suspend fun getAuthState(): Result<Flow<Boolean>, FirebaseAuthError> =
        safeFirebaseAuthCall {
            _currentUserFlow.map { it != null }.distinctUntilChanged()
        }

    override suspend fun signIn(email: String, password: String): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            auth.signInWithEmailAndPassword(email, password).await().user?.reload()?.await()
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await().user?.reload()?.await()
        }

    override suspend fun createAccount(
        email: String,
        password: String
    ): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            auth.createUserWithEmailAndPassword(email, password).await().user?.reload()?.await()
        }

    override suspend fun sendEmailVerification(): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            currentUser?.sendEmailVerification()?.await()
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            auth.sendPasswordResetEmail(email).await()
        }

    override suspend fun reloadUser(): Result<Unit, FirebaseAuthError> =
        safeFirebaseAuthCall {
            currentUser?.reload()?.await()
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

    companion object {
        private const val PROFILE_PHOTO_STORAGE_PATH = "profile_pictures/"
        private const val TAG = "FirebaseAuthRepositoryImpl"
    }
}