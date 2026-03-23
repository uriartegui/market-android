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
        private val CONDOMINIO_ID_KEY = stringPreferencesKey("condominio_id")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]
    }

    val condominioId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CONDOMINIO_ID_KEY]
    }

    suspend fun saveTokens(accessToken: String, condominioId: String?) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            if (condominioId != null) {
                prefs[CONDOMINIO_ID_KEY] = condominioId
            }
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { it.clear() }
    }
}
