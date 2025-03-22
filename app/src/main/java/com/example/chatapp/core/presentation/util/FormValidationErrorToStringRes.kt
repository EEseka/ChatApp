package com.example.chatapp.core.presentation.util

import com.example.chatapp.R
import com.example.chatapp.core.domain.util.FormValidationError

fun FormValidationError.toStringRes(): Int {
    val resId = when (this) {
        FormValidationError.EMPTY_EMAIL -> R.string.error_email_empty
        FormValidationError.INVALID_EMAIL -> R.string.error_email_invalid
        FormValidationError.EMPTY_PASSWORD -> R.string.error_password_empty
        FormValidationError.SHORT_PASSWORD -> R.string.error_password_too_short
        FormValidationError.PASSWORD_TOO_LONG -> R.string.error_password_too_long
        FormValidationError.PASSWORD_MISMATCH -> R.string.error_passwords_do_not_match
        FormValidationError.NO_LOWER_LETTER_IN_PASSWORD -> R.string.error_password_no_letter_lower
        FormValidationError.NO_UPPER_LETTER_IN_PASSWORD -> R.string.error_password_no_letter_upper
        FormValidationError.NO_DIGIT_IN_PASSWORD -> R.string.error_password_no_digit
        FormValidationError.NO_SPECIAL_CHAR_IN_PASSWORD -> R.string.error_password_no_special_char
        FormValidationError.EMPTY_NAME -> R.string.empty_name
        FormValidationError.NAME_TOO_LONG -> R.string.error_name_too_long
        FormValidationError.NAME_TOO_SHORT -> R.string.error_name_too_short
        FormValidationError.INVALID_NAME -> R.string.error_invalid_name
    }
    return resId
}