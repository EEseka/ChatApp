package com.example.chatapp.core.data.firebase

import android.util.Log
import com.example.chatapp.core.domain.util.FirebaseError
import com.example.chatapp.core.domain.util.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal const val TAG = "safeFirebaseCall"

suspend fun <T> safeFirebaseCall(execute: suspend () -> T): Result<T, FirebaseError> {
    return try {
        val result = execute()
        Result.Success(result)
    } catch (e: FirebaseAuthException) {
        if (e is FirebaseAuthInvalidUserException) {
            // For password reset and similar operations where we want to know if the account exists
            Result.Error(FirebaseError.ACCOUNT_NOT_FOUND)
        } else {
            Result.Error(mapFirebaseAuthException(e))
        }
    } catch (e: FirebaseNetworkException) {
        Result.Error(FirebaseError.NETWORK_ERROR)
    } catch (e: Exception) {
        coroutineContext.ensureActive() // Ensure coroutine is still active
        Result.Error(FirebaseError.UNKNOWN)
    }
}

private fun mapFirebaseAuthException(e: FirebaseAuthException): FirebaseError {
    return when (e.errorCode) {
        "ERROR_INVALID_EMAIL" -> FirebaseError.INVALID_EMAIL
        "ERROR_USER_NOT_FOUND" -> FirebaseError.USER_NOT_FOUND
        "ERROR_WRONG_PASSWORD" -> FirebaseError.WRONG_PASSWORD
        "ERROR_EMAIL_ALREADY_IN_USE" -> FirebaseError.EMAIL_ALREADY_IN_USE
        "ERROR_WEAK_PASSWORD" -> FirebaseError.WEAK_PASSWORD
        "ERROR_TOO_MANY_REQUESTS" -> FirebaseError.TOO_MANY_REQUESTS
        "ERROR_OPERATION_NOT_ALLOWED" -> FirebaseError.OPERATION_NOT_ALLOWED
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> FirebaseError.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL
        "ERROR_CREDENTIAL_ALREADY_IN_USE" -> FirebaseError.CREDENTIAL_ALREADY_IN_USE
        "ERROR_INVALID_CREDENTIAL" -> FirebaseError.INVALID_CREDENTIAL
        "ERROR_USER_DISABLED" -> FirebaseError.USER_DISABLED
        "ERROR_REQUIRES_RECENT_LOGIN" -> FirebaseError.REQUIRES_RECENT_LOGIN
        "ERROR_NETWORK_REQUEST_FAILED" -> FirebaseError.NETWORK_ERROR
        "ERROR_USER_TOKEN_EXPIRED" -> FirebaseError.USER_TOKEN_EXPIRED
        "ERROR_INVALID_USER_TOKEN" -> FirebaseError.INVALID_USER_TOKEN
        "ERROR_USER_NOT_SIGNED_IN" -> FirebaseError.USER_NOT_SIGNED_IN
        "ERROR_SESSION_EXPIRED" -> FirebaseError.SESSION_EXPIRED
        "ERROR_QUOTA_EXCEEDED" -> FirebaseError.QUOTA_EXCEEDED
        "ERROR_UNAUTHORIZED_DOMAIN" -> FirebaseError.UNAUTHORIZED_DOMAIN
        "ERROR_INVALID_PHOTO_URL" -> FirebaseError.INVALID_PHOTO_URL
        else -> {
            Log.w(TAG, "Unhandled error code: ${e.errorCode}")
            FirebaseError.UNKNOWN
        }
    }
}