package com.familystalking.app.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.data.datastore.SettingsDataStore
import com.familystalking.app.domain.repository.AuthenticationRepository
import com.familystalking.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository,
    private val settingsDataStore: SettingsDataStore,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isUpdatingUserName = MutableStateFlow(false)
    val isUpdatingUserName: StateFlow<Boolean> = _isUpdatingUserName.asStateFlow()

    private val _locationSharingPreference = MutableStateFlow(true)
    val locationSharingPreference: StateFlow<Boolean> = _locationSharingPreference.asStateFlow()

    private val _isDeviceLocationEnabled = MutableStateFlow(false)
    val isDeviceLocationEnabled: StateFlow<Boolean> = _isDeviceLocationEnabled.asStateFlow()

    private val _pushNotifications = MutableStateFlow(false)
    val pushNotifications: StateFlow<Boolean> = _pushNotifications.asStateFlow()

    private val _showBatteryPercentage = MutableStateFlow(true)
    val showBatteryPercentage: StateFlow<Boolean> = _showBatteryPercentage.asStateFlow()

    init {
        Log.d("SettingsViewModel", "ViewModel initialized.")
        loadUserProfile()
        loadAllDataStoreSettings()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val userId = authenticationRepository.getCurrentUserId()
                if (userId != null) {
                    Log.d("SettingsViewModel", "Fetching profile for User ID: $userId")
                    val userProfile = profileRepository.getUserProfile(userId)
                    if (userProfile?.username?.isNotBlank() == true) {
                        _userName.value = userProfile.username!!
                        Log.i("SettingsViewModel", "Username loaded: ${_userName.value}")
                    } else {
                        Log.w("SettingsViewModel", "Username not found or is blank for user $userId. Using default: '${_userName.value}'")
                    }
                } else {
                    Log.w("SettingsViewModel", "Current user ID is null. Cannot load profile. Using default: '${_userName.value}'")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading user profile: ${e.message}", e)
            }
        }
    }

    fun updateUsername(newUsername: String) {
        if (newUsername.trim() == _userName.value.trim() || newUsername.isBlank()) {
            if(newUsername.isBlank()){
                Log.w("SettingsViewModel", "Attempted to update to a blank username. Aborting.")
            } else {
                Log.d("SettingsViewModel", "Username is the same. No update needed.")
            }
            return
        }

        viewModelScope.launch {
            _isUpdatingUserName.value = true
            try {
                val userId = authenticationRepository.getCurrentUserId()
                if (userId == null) {
                    Log.e("SettingsViewModel", "Cannot update username: User ID is null.")
                    _isUpdatingUserName.value = false
                    return@launch
                }

                Log.d("SettingsViewModel", "Attempting to update username for $userId to '${newUsername.trim()}'")
                val success = profileRepository.updateUsername(userId, newUsername.trim())

                if (success) {
                    _userName.value = newUsername.trim()
                    Log.i("SettingsViewModel", "Username updated successfully to: ${newUsername.trim()}")
                } else {
                    Log.e("SettingsViewModel", "Failed to update username in repository for user $userId.")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating username: ${e.message}", e)
            } finally {
                _isUpdatingUserName.value = false
            }
        }
    }

    private fun loadAllDataStoreSettings() {
        Log.d("SettingsViewModel", "Loading settings from DataStore.")
        viewModelScope.launch {
            settingsDataStore.locationSharingPreference.collect { persistedValue ->
                _locationSharingPreference.value = persistedValue
            }
        }
        viewModelScope.launch {
            settingsDataStore.pushNotificationsPreference.collect { persistedValue ->
                _pushNotifications.value = persistedValue
            }
        }
        viewModelScope.launch {
            settingsDataStore.showBatteryPercentagePreference.collect { persistedValue ->
                _showBatteryPercentage.value = persistedValue
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
        }
    }

    fun togglePushNotifications() {
        viewModelScope.launch {
            val newValue = !_pushNotifications.value
            settingsDataStore.savePushNotificationsPreference(newValue)
        }
    }

    fun toggleShowBatteryPercentage() {
        viewModelScope.launch {
            val newValue = !_showBatteryPercentage.value
            settingsDataStore.saveShowBatteryPercentagePreference(newValue)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authenticationRepository.signOut()
                _userName.value = "User"
                Log.d("SettingsViewModel", "Sign out action completed.")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Sign out failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}