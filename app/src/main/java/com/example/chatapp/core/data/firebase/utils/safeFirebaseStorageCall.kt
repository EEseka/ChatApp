package com.example.chatapp.core.data.firebase.utils

import android.util.Log
import com.example.chatapp.core.domain.util.FirebaseStorageError
import  com.example.chatapp.core.domain.util.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.ensureActive
import kotlinx.io.IOException
import kotlin.coroutines.coroutineContext

private const val TAG = "safeFirebaseStorageCall"

suspend fun <T> safeFirebaseStorageCall(execute: suspend () -> T): Result<T, FirebaseStorageError> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: StorageException) {
        Result.Error(mapStorageException(e))
    } catch (e: FirebaseNetworkException) {
        Result.Error(FirebaseStorageError.NETWORK_ERROR)
    } catch (e: IOException) {
        Log.e(TAG, "IO error during Firebase Storage operation", e)
        Result.Error(FirebaseStorageError.IO_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        Log.e(TAG, "An unexpected error occurred during Firebase Storage operation", e)
        Result.Error(FirebaseStorageError.UNKNOWN)
    }
}

private fun mapStorageException(e: StorageException): FirebaseStorageError {
    return when (e.errorCode) {
        StorageException.ERROR_OBJECT_NOT_FOUND -> FirebaseStorageError.OBJECT_NOT_FOUND
        StorageException.ERROR_BUCKET_NOT_FOUND -> FirebaseStorageError.BUCKET_NOT_FOUND
        StorageException.ERROR_PROJECT_NOT_FOUND -> FirebaseStorageError.PROJECT_NOT_FOUND
        StorageException.ERROR_QUOTA_EXCEEDED -> FirebaseStorageError.QUOTA_EXCEEDED
        StorageException.ERROR_NOT_AUTHENTICATED -> FirebaseStorageError.NOT_AUTHENTICATED
        StorageException.ERROR_NOT_AUTHORIZED -> FirebaseStorageError.NOT_AUTHORIZED
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> FirebaseStorageError.RETRY_LIMIT_EXCEEDED
        StorageException.ERROR_INVALID_CHECKSUM -> FirebaseStorageError.INVALID_CHECKSUM
        StorageException.ERROR_CANCELED -> FirebaseStorageError.CANCELED
        else -> {
            Log.w(TAG, "Unhandled Storage error code: ${e.errorCode}, ${e.message}", e)
            FirebaseStorageError.UNKNOWN
        }
    }
}