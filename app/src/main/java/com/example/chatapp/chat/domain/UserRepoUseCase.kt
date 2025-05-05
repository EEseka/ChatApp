package com.example.chatapp.chat.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser

interface UserRepoUseCase {
    val currentUser: FirebaseUser?
    suspend fun signOut(): Result<Unit, FirebaseAuthError>
    suspend fun deleteAccount(): Result<Unit, FirebaseAuthError>
    suspend fun updateProfile(displayName: String?, photoUri: Uri?): Result<Unit, FirebaseAuthError>
    suspend fun reAuthenticateUser(credential: AuthCredential): Result<Unit, FirebaseAuthError>
}