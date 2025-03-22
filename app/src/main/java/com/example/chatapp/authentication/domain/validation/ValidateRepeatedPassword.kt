package com.example.chatapp.authentication.domain.validation

import com.example.chatapp.core.domain.util.FormValidationError

class ValidateRepeatedPassword {

    operator fun invoke(password: String, repeatedPassword: String): ValidationResult {
        if (password != repeatedPassword) {
            return ValidationResult(false, FormValidationError.PASSWORD_MISMATCH)
        }
        return ValidationResult(true)
    }
}