package com.example.chatapp.authentication.domain.validation

import com.example.chatapp.core.domain.util.FormValidationError

class ValidatePassword {

    operator fun invoke(password: String): ValidationResult {
        if (password.isEmpty()) {
            return ValidationResult(false, FormValidationError.EMPTY_PASSWORD)
        }

        if (password.length < 8) {
            return ValidationResult(false, FormValidationError.SHORT_PASSWORD)
        }

        // Maximum length check to prevent potential DOS (Denial Of Service) attacks
        if (password.length > 64) {
            return ValidationResult(false, FormValidationError.PASSWORD_TOO_LONG)
        }

        val containsDigit = password.any { it.isDigit() }
        val containsLowerCaseLetter = password.any { it.isLowerCase() }
        val containsUpperCaseLetter = password.any { it.isUpperCase() }
        val containsSpecialChar = password.any { !it.isLetterOrDigit() }

        return when {
            !containsLowerCaseLetter -> ValidationResult(false, FormValidationError.NO_LOWER_LETTER_IN_PASSWORD)
            !containsUpperCaseLetter -> ValidationResult(false, FormValidationError.NO_UPPER_LETTER_IN_PASSWORD)
            !containsDigit -> ValidationResult(false, FormValidationError.NO_DIGIT_IN_PASSWORD)
            !containsSpecialChar -> ValidationResult(false, FormValidationError.NO_SPECIAL_CHAR_IN_PASSWORD)
            else -> ValidationResult(true)
        }
    }
}