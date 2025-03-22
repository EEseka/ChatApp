package com.example.chatapp.authentication.domain.validation

import android.util.Patterns
import com.example.chatapp.core.domain.util.FormValidationError

class ValidateEmail {

    operator fun invoke(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, FormValidationError.EMPTY_EMAIL)
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, FormValidationError.INVALID_EMAIL)
        }
        return ValidationResult(true)
    }
}