package com.example.chatapp.core.data.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.utils.ObjectUtils
import com.example.chatapp.core.domain.util.CloudinaryError
import com.example.chatapp.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class CloudinaryManager(private val context: Context) {

    private var isInitialized = false

    fun init(cloudName: String, apiKey: String, apiSecret: String): Result<Unit, CloudinaryError> {
        if (isInitialized) return Result.Success(Unit)

        val config = HashMap<String, String>()
        config["cloud_name"] = cloudName
        config["api_key"] = apiKey
        config["api_secret"] = apiSecret
        config["secure"] = "true"

        return try {
            MediaManager.init(context, config)
            isInitialized = true
            Result.Success(Unit)
        } catch (_: Exception) {
            // Try to check if MediaManager is already initialized
            isInitialized = try {
                MediaManager.get() != null
            } catch (_: Exception) {
                false
            }

            return if (isInitialized) {
                Result.Success(Unit)
            } else {
                Result.Error(CloudinaryError.INITIALIZATION_FAILED)
            }
        }
    }

    suspend fun uploadImage(imageUri: Uri, userId: String): Result<String, CloudinaryError> =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                try {
                    // Create a unique identifier for this user's profile photo
                    val publicId = "profile_photos/$userId"

                    val requestId = MediaManager.get().upload(imageUri)
                        .option("public_id", publicId)
                        .option("overwrite", true) // Replace previous image with same ID
                        .callback(object : UploadCallback {
                            override fun onStart(requestId: String) {
                                // Upload started
                            }

                            override fun onProgress(
                                requestId: String,
                                bytes: Long,
                                totalBytes: Long
                            ) {
                                // Upload progress
                            }

                            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                                // Get the secure URL from result data
                                val secureUrl = resultData["secure_url"] as String
                                continuation.resume(Result.Success(secureUrl))
                            }

                            override fun onError(requestId: String, error: ErrorInfo) {
                                Log.e("CloudinaryManager", "Upload error: ${error.description}")
                                continuation.resume(Result.Error(CloudinaryError.UPLOAD_FAILED))
                            }

                            override fun onReschedule(requestId: String, error: ErrorInfo) {
                                // Upload rescheduled
                            }
                        })
                        .dispatch()

                    continuation.invokeOnCancellation {
                        MediaManager.get().cancelRequest(requestId)
                    }
                } catch (_: Exception) {
                    continuation.resume(Result.Error(CloudinaryError.CONNECTION_ERROR))
                }
            }
        }

    suspend fun deleteImage(userId: String): Result<Unit, CloudinaryError> =
        withContext(Dispatchers.IO) {
            try {
                // Create a unique identifier for this user's profile photo
                val publicId = "profile_photos/$userId"

                // Use the synchronous destroy method within the IO dispatcher context
                val result = MediaManager.get().cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
                )
                // Check if the deletion was successful
                if (result["result"] == "ok") {
                    Result.Success(Unit)
                } else {
                    Result.Error(CloudinaryError.DELETION_FAILED)
                }
            } catch (_: Exception) {
                Result.Error(CloudinaryError.CONNECTION_ERROR)
            }
        }
}