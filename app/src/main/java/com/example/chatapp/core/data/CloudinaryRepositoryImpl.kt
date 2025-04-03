package com.example.chatapp.core.data

import android.net.Uri
import com.example.chatapp.core.data.utils.CloudinaryManager
import com.example.chatapp.core.domain.CloudinaryRepoUseCase
import com.example.chatapp.core.domain.util.CloudinaryError
import com.example.chatapp.core.domain.util.Result

class CloudinaryRepositoryImpl(
    private val cloudinaryManager: CloudinaryManager
) : CloudinaryRepoUseCase {

    override suspend fun uploadProfileImage(
        userId: String,
        imageUri: Uri
    ): Result<String, CloudinaryError> =
        cloudinaryManager.uploadImage(imageUri, userId)

    override suspend fun deleteProfileImage(userId: String): Result<Unit, CloudinaryError> =
        cloudinaryManager.deleteImage(userId)
}