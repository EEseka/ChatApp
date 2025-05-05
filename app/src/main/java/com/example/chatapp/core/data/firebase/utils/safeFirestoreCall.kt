package com.example.chatapp.core.data.firebase.utils

import android.util.Log
import com.example.chatapp.core.domain.util.FirebaseFirestoreError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private const val TAG = "safeFirebaseFirestoreCall"

suspend fun <T> safeFirebaseFirestoreCall(execute: suspend () -> T): Result<T, FirebaseFirestoreError> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: FirebaseFirestoreException) {
        Result.Error(mapFirestoreException(e))
    } catch (_: FirebaseNetworkException) {
        Result.Error(FirebaseFirestoreError.NETWORK_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        Log.e(TAG, "An unexpected error occurred", e)
        Result.Error(FirebaseFirestoreError.UNKNOWN)
    }
}

private fun mapFirestoreException(e: FirebaseFirestoreException): FirebaseFirestoreError {
    return when (e.code) {
        FirebaseFirestoreException.Code.CANCELLED -> FirebaseFirestoreError.CANCELLED
        FirebaseFirestoreException.Code.UNKNOWN -> FirebaseFirestoreError.UNKNOWN
        FirebaseFirestoreException.Code.INVALID_ARGUMENT -> FirebaseFirestoreError.INVALID_ARGUMENT
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FirebaseFirestoreError.DEADLINE_EXCEEDED
        FirebaseFirestoreException.Code.NOT_FOUND -> FirebaseFirestoreError.NOT_FOUND
        FirebaseFirestoreException.Code.ALREADY_EXISTS -> FirebaseFirestoreError.ALREADY_EXISTS
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirebaseFirestoreError.PERMISSION_DENIED
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> FirebaseFirestoreError.RESOURCE_EXHAUSTED
        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> FirebaseFirestoreError.FAILED_PRECONDITION
        FirebaseFirestoreException.Code.ABORTED -> FirebaseFirestoreError.ABORTED
        FirebaseFirestoreException.Code.OUT_OF_RANGE -> FirebaseFirestoreError.OUT_OF_RANGE
        FirebaseFirestoreException.Code.UNIMPLEMENTED -> FirebaseFirestoreError.UNIMPLEMENTED
        FirebaseFirestoreException.Code.INTERNAL -> FirebaseFirestoreError.INTERNAL
        FirebaseFirestoreException.Code.UNAVAILABLE -> FirebaseFirestoreError.UNAVAILABLE
        FirebaseFirestoreException.Code.DATA_LOSS -> FirebaseFirestoreError.DATA_LOSS
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> FirebaseFirestoreError.UNAUTHENTICATED
        else -> {
            Log.w(TAG, "Unhandled Firestore error code: ${e.code}")
            FirebaseFirestoreError.UNKNOWN
        }
    }
}