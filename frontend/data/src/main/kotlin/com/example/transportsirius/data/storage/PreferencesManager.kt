package com.example.transportsirius.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Расширение для Context, создающее DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "transport_sirius_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // Ключи для хранения данных
    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val IS_ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("is_onboarding_completed")
        private val LAST_SOURCE_KEY = stringPreferencesKey("last_source")
        private val LAST_DESTINATION_KEY = stringPreferencesKey("last_destination")
    }
    
    // Сохранение ID пользователя
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }
    
    // Получение ID пользователя
    val userIdFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY] ?: ""
        }
    
    // Сохранение имени пользователя
    suspend fun saveUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = userName
        }
    }
    
    // Получение имени пользователя
    val userNameFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }
    
    // Сохранение статуса онбординга
    suspend fun saveOnboardingStatus(isCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED_KEY] = isCompleted
        }
    }
    
    // Получение статуса онбординга
    val onboardingStatusFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED_KEY] ?: false
        }
    
    // Сохранение последнего адреса источника
    suspend fun saveLastSource(source: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SOURCE_KEY] = source
        }
    }
    
    // Получение последнего адреса источника
    val lastSourceFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_SOURCE_KEY] ?: ""
        }
    
    // Сохранение последнего адреса назначения
    suspend fun saveLastDestination(destination: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_DESTINATION_KEY] = destination
        }
    }
    
    // Получение последнего адреса назначения
    val lastDestinationFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_DESTINATION_KEY] ?: ""
        }
    
    // Очистка всех предпочтений
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
} 