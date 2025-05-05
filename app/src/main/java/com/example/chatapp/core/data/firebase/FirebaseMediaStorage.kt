package com.example.chatapp.core.data.firebase

import android.net.Uri
import android.util.Log
import com.example.chatapp.core.data.firebase.utils.safeFirebaseStorageCall
import com.example.chatapp.core.domain.MediaStorage
import com.example.chatapp.core.domain.util.FirebaseStorageError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseMediaStorage(private val storage: FirebaseStorage) : MediaStorage {
    override suspend fun uploadPicture(
        storagePath: String,
        imageUri: Uri
    ): Result<String, FirebaseStorageError> {
        if (storagePath.isBlank() || !storagePath.contains('/')) {
            Log.e(TAG, "Invalid storage path provided: $storagePath")
            return Result.Error(FirebaseStorageError.INVALID_CHECKSUM)
        }
        return safeFirebaseStorageCall {
            val storageRef = storage.reference.child(storagePath)

            storageRef.putFile(imageUri).await()
            Log.i(TAG, "Successfully uploaded to $storagePath")

            val downloadUrl = storageRef.downloadUrl.await()
            Log.i(TAG, "Successfully retrieved download URL for $storagePath")
            downloadUrl.toString()
        }
    }

    override suspend fun getPicture(storagePath: String): Result<String, FirebaseStorageError> {
        if (storagePath.isBlank() || !storagePath.contains('/')) {
            Log.e(TAG, "Invalid storage path provided: $storagePath")
            return Result.Error(FirebaseStorageError.INVALID_CHECKSUM)
        }
        return safeFirebaseStorageCall {
            val storageRef = storage.reference.child(storagePath)
            val downloadUrl = storageRef.downloadUrl.await()
            Log.i(TAG, "Successfully retrieved download URL for $storagePath")
            downloadUrl.toString()
        }
    }

    override suspend fun deletePicture(storagePath: String): Result<Unit, FirebaseStorageError> {
        if (storagePath.isBlank()) {
            Log.e(TAG, "Invalid storage path provided for deletion: $storagePath")
            return Result.Error(FirebaseStorageError.INVALID_CHECKSUM)
        }
        return safeFirebaseStorageCall {
            val storageRef = storage.reference.child(storagePath)
            storageRef.delete().await()
            Log.i(TAG, "Successfully deleted $storagePath")
            Unit
        }
    }

    override suspend fun deleteAllPictures(storagePath: String): Result<Unit, FirebaseStorageError> {
        if (storagePath.isBlank()) {
            Log.e(TAG, "No storage paths provided for deletion: $storagePath")
            return Result.Error(FirebaseStorageError.INVALID_CHECKSUM)
        }
        return safeFirebaseStorageCall {
            val storageRef = storage.reference.child(storagePath)
            storageRef.listAll().await().items.forEach { item ->
                item.delete().await()
                Log.i(TAG, "Successfully deleted: ${item.path}")
            }
            Log.i(TAG, "Successfully deleted all images under $storagePath")
            Unit
        }
    }

    companion object {
        private const val TAG = "FirebaseMediaStorage"
    }
}
