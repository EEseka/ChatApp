package com.example.chatapp.authentication.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.chatapp.authentication.domain.CheckFirstInstallUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class CheckFirstInstallDataSource(private val context: Context) : CheckFirstInstallUseCase {
    private companion object {
        private const val TAG = "CheckFirstInstallDataSource"
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "app_preferences"
        )

        private object PreferencesKeys {
            val FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
            val ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        }
    }

    override suspend fun invoke(): Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading preferences", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val isFirstLaunch = preferences[PreferencesKeys.FIRST_LAUNCH] != false
            val isOnboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] == true

            isFirstLaunch || !isOnboardingCompleted
        }

    override suspend fun markOnboardingComplete() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = false
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun resetFirstLaunchState() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH] = true
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = false
        }
    }
}