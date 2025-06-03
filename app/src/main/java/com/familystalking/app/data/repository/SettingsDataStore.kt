package com.familystalking.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Top-level property for the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings_preferences") // Changed name slightly for clarity

@Singleton
class SettingsDataStore @Inject constructor(private val context: Context) {

    private object PreferencesKeys {
        val LOCATION_SHARING_PREFERENCE = booleanPreferencesKey("location_sharing_preference")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        val SHOW_BATTERY_PERCENTAGE = booleanPreferencesKey("show_battery_percentage_on_map") // Made key more specific
    }

    val locationSharingPreference: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LOCATION_SHARING_PREFERENCE] ?: true // Default
        }

    suspend fun saveLocationSharingPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCATION_SHARING_PREFERENCE] = isEnabled
        }
    }

    val pushNotificationsPreference: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PUSH_NOTIFICATIONS] ?: false // Default
        }

    suspend fun savePushNotificationsPreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PUSH_NOTIFICATIONS] = isEnabled
        }
    }

    // This is the flow your MapViewModel will observe
    val showBatteryPercentagePreference: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY_PERCENTAGE] ?: true // Default: show battery
        }

    suspend fun saveShowBatteryPercentagePreference(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_BATTERY_PERCENTAGE] = isEnabled
        }
    }
}