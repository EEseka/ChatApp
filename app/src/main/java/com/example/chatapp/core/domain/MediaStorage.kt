package com.example.chatapp.core.domain

import android.net.Uri
import com.example.chatapp.core.domain.util.FirebaseStorageError
import com.example.chatapp.core.domain.util.Result

interface MediaStorage {
    suspend fun uploadPicture(
        storagePath: String,
        imageUri: Uri
    ): Result<String, FirebaseStorageError>

    suspend fun getPicture(storagePath: String): Result<String, FirebaseStorageError>
    suspend fun deletePicture(storagePath: String): Result<Unit, FirebaseStorageError>
    suspend fun deleteAllPictures(storagePath: String): Result<Unit, FirebaseStorageError>
}