package com.example.chatapp.authentication.domain.validation

import com.example.chatapp.core.domain.util.FormValidationError

class ValidateDisplayName {

    operator fun invoke(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult(false, FormValidationError.EMPTY_NAME)
        }
        if (name.length < 3) {
            return ValidationResult(false, FormValidationError.NAME_TOO_SHORT)
        }
        if (name.length > 30) {
            return ValidationResult(false, FormValidationError.NAME_TOO_LONG)
        }
        if (!name.matches(Regex("^[a-zA-Z0-9\\s]*$"))) {
            return ValidationResult(false, FormValidationError.INVALID_NAME)
        }
        return ValidationResult(true)
    }
}
