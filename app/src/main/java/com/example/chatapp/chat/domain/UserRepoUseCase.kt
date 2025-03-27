package com.example.chatapp.chat.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.FirebaseError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.auth.FirebaseUser

interface UserRepoUseCase {
    val currentUser: FirebaseUser?
    suspend fun signOut(): Result<Unit, FirebaseError>
    suspend fun deleteAccount(): Result<Unit, FirebaseError>
    suspend fun updateProfile(displayName: String?, photoUri: Uri?): Result<Unit, FirebaseError>
}