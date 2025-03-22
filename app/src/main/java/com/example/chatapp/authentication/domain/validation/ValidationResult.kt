package com.example.chatapp.authentication.domain.validation

import com.example.chatapp.core.domain.util.FormValidationError

data class ValidationResult(
    val successful: Boolean,
    val errorMessage: FormValidationError? = null
)