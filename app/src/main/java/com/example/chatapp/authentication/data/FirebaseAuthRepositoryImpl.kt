package com.example.chatapp.authentication.data

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.example.chatapp.authentication.domain.UserAuthUseCase
import com.example.chatapp.core.data.firebase.safeFirebaseCall
import com.example.chatapp.core.domain.CloudinaryRepoUseCase
import com.example.chatapp.core.domain.util.FirebaseError
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
    private val cloudinaryRepo: CloudinaryRepoUseCase
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

    override suspend fun getAuthState(): Result<Flow<Boolean>, FirebaseError> =
        safeFirebaseCall {
            _currentUserFlow.map { it != null }.distinctUntilChanged()
        }

    override suspend fun signIn(email: String, password: String): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            auth.signInWithEmailAndPassword(email, password).await().user?.reload()?.await()
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await().user?.reload()?.await()
        }

    override suspend fun createAccount(
        email: String,
        password: String
    ): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            auth.createUserWithEmailAndPassword(email, password).await().user?.reload()?.await()
        }

    override suspend fun sendEmailVerification(): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            currentUser?.sendEmailVerification()?.await()
        }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            auth.sendPasswordResetEmail(email).await()
        }

    override suspend fun reloadUser(): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            currentUser?.reload()?.await()
        }

    override suspend fun updateProfile(
        displayName: String?,
        photoUri: Uri?
    ): Result<Unit, FirebaseError> =
        safeFirebaseCall {
            val userId = currentUser?.uid
            var finalPhotoUrl: String? = null

            // If we have a new photo URI, upload it to Cloudinary
            if (photoUri != null && userId != null) {
                cloudinaryRepo.uploadProfileImage(userId, photoUri)
                    .onSuccess { secureUrl ->
                        finalPhotoUrl = secureUrl
                    }
                    .onError { error ->
                        Log.e(
                            "FirebaseAuthRepositoryImpl",
                            "Failed to upload profile image to Cloudinary : $error"
                        )
                        Result.Error(FirebaseError.IMAGE_UPLOAD_FAILED)
                    }
            }

            val profileUpdates = userProfileChangeRequest {
                displayName?.let { this.displayName = it }
                finalPhotoUrl?.let { this.photoUri = it.toUri() }
            }
            currentUser?.updateProfile(profileUpdates)?.await()
            currentUser?.reload()?.await()
        }
}