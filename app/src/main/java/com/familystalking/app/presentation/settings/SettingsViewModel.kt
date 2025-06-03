package com.familystalking.app.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.data.datastore.SettingsDataStore
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // To get a single value
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userName = MutableStateFlow("User") // Default, load real name
    val userName: StateFlow<String> = _userName.asStateFlow()

    // Internal MutableStateFlows, initialized with a sensible default or loading state
    private val _locationSharingPreference = MutableStateFlow(true) // Initial default
    val locationSharingPreference: StateFlow<Boolean> = _locationSharingPreference.asStateFlow()

    private val _isDeviceLocationEnabled = MutableStateFlow(false)
    val isDeviceLocationEnabled: StateFlow<Boolean> = _isDeviceLocationEnabled.asStateFlow()

    private val _pushNotifications = MutableStateFlow(false) // Initial default
    val pushNotifications: StateFlow<Boolean> = _pushNotifications.asStateFlow()

    private val _showBatteryPercentage = MutableStateFlow(true) // Initial default
    val showBatteryPercentage: StateFlow<Boolean> = _showBatteryPercentage.asStateFlow()

    init {
        Log.d("SettingsViewModel", "ViewModel initialized. Loading settings from DataStore.")
        // Load all settings from DataStore when ViewModel is created
        loadAllSettings()
        // TODO: Load username
    }

    private fun loadAllSettings() {
        viewModelScope.launch {
            // Location Sharing
            settingsDataStore.locationSharingPreference.collect { persistedValue ->
                _locationSharingPreference.value = persistedValue
                Log.d("SettingsViewModel", "Loaded Location Sharing Preference: $persistedValue")
            }
        }
        viewModelScope.launch {
            // Push Notifications
            settingsDataStore.pushNotificationsPreference.collect { persistedValue ->
                _pushNotifications.value = persistedValue
                Log.d("SettingsViewModel", "Loaded Push Notifications Preference: $persistedValue")
            }
        }
        viewModelScope.launch {
            // Show Battery Percentage
            settingsDataStore.showBatteryPercentagePreference.collect { persistedValue ->
                _showBatteryPercentage.value = persistedValue
                Log.d("SettingsViewModel", "Loaded Show Battery Preference: $persistedValue")
            }
        }
    }

    fun updateDeviceLocationStatus(isEnabled: Boolean) {
        _isDeviceLocationEnabled.value = isEnabled
    }

    fun toggleLocationSharingPreference() {
        viewModelScope.launch {
            val newValue = !_locationSharingPreference.value
            settingsDataStore.saveLocationSharingPreference(newValue)
            // The collector in loadAllSettings will update _locationSharingPreference.value
            Log.d("SettingsViewModel", "Location Sharing Preference Toggled & Saved: $newValue")
        }
    }

    fun togglePushNotifications() {
        viewModelScope.launch {
            val newValue = !_pushNotifications.value
            settingsDataStore.savePushNotificationsPreference(newValue)
            Log.d("SettingsViewModel", "Push Notifications Toggled & Saved: $newValue")
        }
    }

    fun toggleShowBatteryPercentage() {
        viewModelScope.launch {
            val newValue = !_showBatteryPercentage.value
            settingsDataStore.saveShowBatteryPercentagePreference(newValue)
            Log.d("SettingsViewModel", "Show Battery Toggled & Saved: $newValue")
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authenticationRepository.signOut()
                Log.d("SettingsViewModel", "Sign out action completed.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Sign out failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}