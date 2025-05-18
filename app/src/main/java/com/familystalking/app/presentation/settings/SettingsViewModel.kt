package com.familystalking.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.repository.AuthenticationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userName = MutableStateFlow("Bert")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _locationSharing = MutableStateFlow(true)
    val locationSharing: StateFlow<Boolean> = _locationSharing.asStateFlow()

    private val _pushNotifications = MutableStateFlow(false)
    val pushNotifications: StateFlow<Boolean> = _pushNotifications.asStateFlow()

    private val _showBatteryPercentage = MutableStateFlow(true)
    val showBatteryPercentage: StateFlow<Boolean> = _showBatteryPercentage.asStateFlow()

    fun toggleLocationSharing() {
        _locationSharing.value = !_locationSharing.value
    }

    fun togglePushNotifications() {
        _pushNotifications.value = !_pushNotifications.value
    }

    fun toggleShowBatteryPercentage() {
        _showBatteryPercentage.value = !_showBatteryPercentage.value
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            authenticationRepository.signOut()
            _isLoading.value = false
        }
    }
}
