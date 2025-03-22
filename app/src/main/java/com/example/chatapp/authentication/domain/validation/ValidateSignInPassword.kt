package com.example.chatapp.authentication.domain.validation

import com.example.chatapp.core.domain.util.FormValidationError

class ValidateSignInPassword {

    operator fun invoke(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, FormValidationError.EMPTY_PASSWORD)
        }
        return ValidationResult(true)
    }
}
