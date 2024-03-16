package com.bari_ikutsu.lnmsgbridge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrefStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("userToken")
        private val NOTIFICATION_ACCESS_KEY = booleanPreferencesKey("notificationAccess")
        private val NOTIFICATION_TIMEOUT_ENABLED_KEY = booleanPreferencesKey("notificationTimeoutEnabled")
        private val NOTIFICATION_TIMEOUT_KEY = floatPreferencesKey("notificationTimeout")
    }

    val getNotificationAccess: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_ACCESS_KEY] ?: false
    }
    val getNotificationTimeoutEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_TIMEOUT_ENABLED_KEY] ?: true
    }
    val getNotificationTimeout: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_TIMEOUT_KEY] ?: 3.0f
    }

    suspend fun saveNotificationAccess(notificationAccess: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ACCESS_KEY] = notificationAccess
        }
    }

    suspend fun saveNotificationTimeoutEnabled(notificationTimeoutEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIMEOUT_ENABLED_KEY] = notificationTimeoutEnabled
        }
    }

    suspend fun saveNotificationTimeout(notificationTimeout: Float) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_TIMEOUT_KEY] = notificationTimeout
        }
    }
}