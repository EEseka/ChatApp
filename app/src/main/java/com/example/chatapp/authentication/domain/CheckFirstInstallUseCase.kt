package com.example.chatapp.authentication.domain

import kotlinx.coroutines.flow.Flow

interface CheckFirstInstallUseCase {
    suspend operator fun invoke(): Flow<Boolean>
    suspend fun markOnboardingComplete()
    suspend fun resetFirstLaunchState()
}