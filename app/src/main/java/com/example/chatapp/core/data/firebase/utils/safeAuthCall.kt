package com.example.chatapp.core.data.firebase.utils

import android.util.Log
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

private const val TAG = "safeFirebaseAuthCall"

suspend fun <T> safeFirebaseAuthCall(execute: suspend () -> T): Result<T, FirebaseAuthError> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: FirebaseAuthException) {
        Result.Error(mapFirebaseAuthException(e))
    } catch (_: FirebaseNetworkException) {
        Result.Error(FirebaseAuthError.NETWORK_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        Log.e(TAG, "An unexpected error occurred", e)
        Result.Error(FirebaseAuthError.UNKNOWN)
    }
}

private fun mapFirebaseAuthException(e: FirebaseAuthException): FirebaseAuthError {
    return when (e.errorCode) {
        "ERROR_INVALID_EMAIL" -> FirebaseAuthError.INVALID_EMAIL
        "ERROR_USER_NOT_FOUND" -> FirebaseAuthError.USER_NOT_FOUND
        "ERROR_WRONG_PASSWORD" -> FirebaseAuthError.WRONG_PASSWORD
        "ERROR_EMAIL_ALREADY_IN_USE" -> FirebaseAuthError.EMAIL_ALREADY_IN_USE
        "ERROR_WEAK_PASSWORD" -> FirebaseAuthError.WEAK_PASSWORD
        "ERROR_TOO_MANY_REQUESTS" -> FirebaseAuthError.TOO_MANY_REQUESTS
        "ERROR_OPERATION_NOT_ALLOWED" -> FirebaseAuthError.OPERATION_NOT_ALLOWED
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> FirebaseAuthError.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL
        "ERROR_CREDENTIAL_ALREADY_IN_USE" -> FirebaseAuthError.CREDENTIAL_ALREADY_IN_USE
        "ERROR_INVALID_CREDENTIAL" -> FirebaseAuthError.INVALID_CREDENTIAL
        "ERROR_REQUIRES_RECENT_LOGIN" -> FirebaseAuthError.REQUIRES_RECENT_LOGIN
        "ERROR_NETWORK_REQUEST_FAILED" -> FirebaseAuthError.NETWORK_ERROR
        "ERROR_USER_TOKEN_EXPIRED" -> FirebaseAuthError.USER_TOKEN_EXPIRED
        "ERROR_INVALID_USER_TOKEN" -> FirebaseAuthError.INVALID_USER_TOKEN
        "ERROR_USER_NOT_SIGNED_IN" -> FirebaseAuthError.USER_NOT_SIGNED_IN
        "ERROR_SESSION_EXPIRED" -> FirebaseAuthError.SESSION_EXPIRED
        "ERROR_QUOTA_EXCEEDED" -> FirebaseAuthError.QUOTA_EXCEEDED
        "ERROR_UNAUTHORIZED_DOMAIN" -> FirebaseAuthError.UNAUTHORIZED_DOMAIN
        "ERROR_INVALID_PHOTO_URL" -> FirebaseAuthError.INVALID_PHOTO_URL
        "ERROR_USER_DISABLED" -> FirebaseAuthError.USER_DISABLED
        else -> {
            Log.w(TAG, "Unhandled error code: ${e.errorCode}")
            FirebaseAuthError.UNKNOWN
        }
    }
}