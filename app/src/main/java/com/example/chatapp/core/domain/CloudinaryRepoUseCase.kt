package com.example.chatapp.core.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.CloudinaryError
import com.example.chatapp.core.domain.util.Result

interface CloudinaryRepoUseCase {
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String, CloudinaryError>
    suspend fun deleteProfileImage(userId: String): Result<Unit, CloudinaryError>
}