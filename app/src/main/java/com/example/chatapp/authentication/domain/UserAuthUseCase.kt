package com.example.chatapp.authentication.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface UserAuthUseCase {
    val currentUser: FirebaseUser?
    suspend fun getAuthState(): Result<Flow<Boolean>, FirebaseAuthError>
    suspend fun signIn(email: String, password: String): Result<Unit, FirebaseAuthError>
    suspend fun signInWithGoogle(idToken: String): Result<Unit, FirebaseAuthError>
    suspend fun createAccount(email: String, password: String): Result<Unit, FirebaseAuthError>
    suspend fun sendEmailVerification(): Result<Unit, FirebaseAuthError>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit, FirebaseAuthError>
    suspend fun reloadUser(): Result<Unit, FirebaseAuthError>
    suspend fun updateProfile(displayName: String?, photoUri: Uri?): Result<Unit, FirebaseAuthError>
}