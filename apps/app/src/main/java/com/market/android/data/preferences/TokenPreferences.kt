package com.market.android.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "market_prefs")

class TokenPreferences(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val CONDOMINIO_ID_KEY = stringPreferencesKey("condominio_id")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val MANAGER_PIN_KEY = stringPreferencesKey("manager_pin")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]
    }

    val condominioId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CONDOMINIO_ID_KEY]
    }

    val userRole: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ROLE_KEY]
    }

    val managerPin: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[MANAGER_PIN_KEY]
    }

    suspend fun saveManagerPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[MANAGER_PIN_KEY] = pin
        }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?, condominioId: String?, userRole: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            if (refreshToken != null) prefs[REFRESH_TOKEN_KEY] = refreshToken
            if (condominioId != null) prefs[CONDOMINIO_ID_KEY] = condominioId
            if (userRole != null) prefs[USER_ROLE_KEY] = userRole
        }
    }

    suspend fun updateAccessToken(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            // Preserve the manager PIN across logouts
            val pin = prefs[MANAGER_PIN_KEY]
            prefs.clear()
            if (pin != null) prefs[MANAGER_PIN_KEY] = pin
        }
    }
}
