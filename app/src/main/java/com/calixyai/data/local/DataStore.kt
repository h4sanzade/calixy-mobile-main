package com.calixyai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calixy_user_prefs")

@Singleton
class FirstTimeUserStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_IS_FIRST_TIME = booleanPreferencesKey("is_first_time_user")
    }

    val isFirstTimeUser: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_IS_FIRST_TIME] ?: true }

    suspend fun markFirstTime() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FIRST_TIME] = true
        }
    }

    suspend fun markNotFirstTime() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_FIRST_TIME] = false
        }
    }
}