package com.example.chatapp.core.presentation.util

import android.content.Context
import com.example.chatapp.R
import com.example.chatapp.core.domain.util.FirebaseAuthError
import com.example.chatapp.core.domain.util.FirebaseFirestoreError
import com.example.chatapp.core.domain.util.FirebaseStorageError

fun FirebaseAuthError.toString(context: Context): String {
    val resId = when (this) {
        FirebaseAuthError.INVALID_EMAIL -> R.string.error_invalid_email
        FirebaseAuthError.USER_NOT_FOUND -> R.string.account_not_found
        FirebaseAuthError.WRONG_PASSWORD -> R.string.error_wrong_password
        FirebaseAuthError.EMAIL_ALREADY_IN_USE -> R.string.error_email_already_in_use
        FirebaseAuthError.WEAK_PASSWORD -> R.string.error_weak_password
        FirebaseAuthError.NETWORK_ERROR -> R.string.error_network_error
        FirebaseAuthError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests_for_firebase
        FirebaseAuthError.UNKNOWN -> R.string.error_unknown
        FirebaseAuthError.OPERATION_NOT_ALLOWED -> R.string.error_operation_not_allowed
        FirebaseAuthError.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> R.string.error_account_exists_with_different_credential
        FirebaseAuthError.CREDENTIAL_ALREADY_IN_USE -> R.string.error_credential_already_in_use
        FirebaseAuthError.INVALID_CREDENTIAL -> R.string.error_invalid_credential
        FirebaseAuthError.USER_DISABLED -> R.string.error_user_disabled
        FirebaseAuthError.REQUIRES_RECENT_LOGIN -> R.string.error_requires_recent_login
        FirebaseAuthError.INVALID_LINK -> R.string.error_invalid_link
        FirebaseAuthError.USER_TOKEN_EXPIRED -> R.string.error_user_token_expired
        FirebaseAuthError.INVALID_USER_TOKEN -> R.string.error_invalid_user_token
        FirebaseAuthError.USER_NOT_SIGNED_IN -> R.string.error_user_not_signed_in
        FirebaseAuthError.SESSION_EXPIRED -> R.string.error_session_expired
        FirebaseAuthError.QUOTA_EXCEEDED -> R.string.error_quota_exceeded
        FirebaseAuthError.UNAUTHORIZED_DOMAIN -> R.string.error_unauthorized_domain
        FirebaseAuthError.INVALID_PHOTO_URL -> R.string.error_invalid_photo_url
        FirebaseAuthError.FAILED_REAUTHENTICATION -> R.string.error_failed_reauthentication
        FirebaseAuthError.GOOGLE_SIGN_IN_FAILED -> R.string.error_google_sign_in_failed
        FirebaseAuthError.CREDENTIAL_CREATION_ERROR -> R.string.error_credential_creation_error
        FirebaseAuthError.CREDENTIAL_FETCHING_ERROR -> R.string.error_credential_fetching_error
        FirebaseAuthError.IMAGE_UPLOAD_FAILED -> R.string.error_image_upload_failed
        FirebaseAuthError.IMAGE_DELETE_FAILED -> R.string.error_image_delete_failed
        FirebaseAuthError.CREDENTIAL_NOT_FOUND -> R.string.error_credential_not_found
    }
    return context.getString(resId)
}

fun FirebaseFirestoreError.toString(context: Context): String {
    val resId = when (this) {
        FirebaseFirestoreError.CANCELLED -> R.string.error_cancelled
        FirebaseFirestoreError.UNKNOWN -> R.string.error_unknown
        FirebaseFirestoreError.INVALID_ARGUMENT -> R.string.error_invalid_argument
        FirebaseFirestoreError.DEADLINE_EXCEEDED -> R.string.error_deadline_exceeded
        FirebaseFirestoreError.NETWORK_ERROR -> R.string.error_network_error
        FirebaseFirestoreError.NOT_FOUND -> R.string.error_not_found
        FirebaseFirestoreError.ALREADY_EXISTS -> R.string.error_already_exists
        FirebaseFirestoreError.PERMISSION_DENIED -> R.string.error_permission_denied
        FirebaseFirestoreError.RESOURCE_EXHAUSTED -> R.string.error_resource_exhausted
        FirebaseFirestoreError.FAILED_PRECONDITION -> R.string.error_failed_precondition
        FirebaseFirestoreError.ABORTED -> R.string.error_aborted
        FirebaseFirestoreError.OUT_OF_RANGE -> R.string.error_out_of_range
        FirebaseFirestoreError.UNIMPLEMENTED -> R.string.error_unimplemented
        FirebaseFirestoreError.INTERNAL -> R.string.error_internal
        FirebaseFirestoreError.UNAVAILABLE -> R.string.error_unavailable
        FirebaseFirestoreError.DATA_LOSS -> R.string.error_data_loss
        FirebaseFirestoreError.UNAUTHENTICATED -> R.string.error_unauthenticated
    }
    return context.getString(resId)
}

fun FirebaseStorageError.toString(context: Context): String {
    val resId = when (this) {
        FirebaseStorageError.UNKNOWN -> R.string.error_unknown
        FirebaseStorageError.OBJECT_NOT_FOUND -> R.string.error_object_not_found
        FirebaseStorageError.BUCKET_NOT_FOUND -> R.string.error_bucket_not_found
        FirebaseStorageError.PROJECT_NOT_FOUND -> R.string.error_project_not_found
        FirebaseStorageError.QUOTA_EXCEEDED -> R.string.error_quota_exceeded
        FirebaseStorageError.NOT_AUTHENTICATED -> R.string.error_unauthenticated
        FirebaseStorageError.NOT_AUTHORIZED -> R.string.error_permission_denied
        FirebaseStorageError.RETRY_LIMIT_EXCEEDED -> R.string.error_retry_limit_exceeded
        FirebaseStorageError.INVALID_CHECKSUM -> R.string.error_invalid_checksum
        FirebaseStorageError.CANCELED -> R.string.error_canceled
        FirebaseStorageError.NETWORK_ERROR -> R.string.error_network_error
        FirebaseStorageError.IO_ERROR -> R.string.error_io_error
    }
    return context.getString(resId)
}