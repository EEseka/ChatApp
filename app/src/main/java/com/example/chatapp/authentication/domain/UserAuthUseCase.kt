package com.example.chatapp.authentication.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.FirebaseError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface UserAuthUseCase {
    val currentUser: FirebaseUser?
    suspend fun getAuthState(): Result<Flow<Boolean>, FirebaseError>
    suspend fun signIn(email: String, password: String): Result<Unit, FirebaseError>
    suspend fun signInWithGoogle(idToken: String): Result<Unit, FirebaseError>
    suspend fun createAccount(email: String, password: String): Result<Unit, FirebaseError>
    suspend fun sendEmailVerification(): Result<Unit, FirebaseError>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit, FirebaseError>
    suspend fun signOut(): Result<Unit, FirebaseError>
    suspend fun deleteAccount(): Result<Unit, FirebaseError>
    suspend fun reloadUser(): Result<Unit, FirebaseError>
    suspend fun updateProfile(displayName: String?, photoUri: Uri?): Result<Unit, FirebaseError>
    suspend fun updateEmail(newEmail: String): Result<Unit, FirebaseError>
}