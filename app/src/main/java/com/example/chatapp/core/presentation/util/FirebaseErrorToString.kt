package com.example.chatapp.core.presentation.util

import android.content.Context
import com.example.chatapp.R
import com.example.chatapp.core.domain.util.FirebaseError

fun FirebaseError.toString(context: Context): String {
    val resId = when (this) {
        FirebaseError.INVALID_EMAIL -> R.string.error_invalid_email
        FirebaseError.USER_NOT_FOUND -> R.string.account_not_found
        FirebaseError.WRONG_PASSWORD -> R.string.error_wrong_password
        FirebaseError.EMAIL_ALREADY_IN_USE -> R.string.error_email_already_in_use
        FirebaseError.WEAK_PASSWORD -> R.string.error_weak_password
        FirebaseError.NETWORK_ERROR -> R.string.error_network_error
        FirebaseError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests_for_firebase
        FirebaseError.UNKNOWN -> R.string.error_unknown
        FirebaseError.OPERATION_NOT_ALLOWED -> R.string.error_operation_not_allowed
        FirebaseError.ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL -> R.string.error_account_exists_with_different_credential
        FirebaseError.CREDENTIAL_ALREADY_IN_USE -> R.string.error_credential_already_in_use
        FirebaseError.INVALID_CREDENTIAL -> R.string.error_invalid_credential
        FirebaseError.USER_DISABLED -> R.string.error_user_disabled
        FirebaseError.REQUIRES_RECENT_LOGIN -> R.string.error_requires_recent_login
        FirebaseError.INVALID_LINK -> R.string.error_invalid_link
        FirebaseError.USER_TOKEN_EXPIRED -> R.string.error_user_token_expired
        FirebaseError.INVALID_USER_TOKEN -> R.string.error_invalid_user_token
        FirebaseError.USER_NOT_SIGNED_IN -> R.string.error_user_not_signed_in
        FirebaseError.SESSION_EXPIRED -> R.string.error_session_expired
        FirebaseError.QUOTA_EXCEEDED -> R.string.error_quota_exceeded
        FirebaseError.UNAUTHORIZED_DOMAIN -> R.string.error_unauthorized_domain
        FirebaseError.INVALID_PHOTO_URL -> R.string.error_invalid_photo_url
        FirebaseError.FAILED_REAUTHENTICATION -> R.string.error_failed_reauthentication
        FirebaseError.GOOGLE_SIGN_IN_FAILED -> R.string.error_google_sign_in_failed
        FirebaseError.CREDENTIAL_CREATION_ERROR -> R.string.error_credential_creation_error
        FirebaseError.CREDENTIAL_FETCHING_ERROR -> R.string.error_credential_fetching_error
    }
    return context.getString(resId)
}