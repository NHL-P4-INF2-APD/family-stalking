package com.familystalking.app.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

private const val MIN_BATTERY_PERCENTAGE = 0
private const val MAX_BATTERY_PERCENTAGE = 100
private const val INITIAL_BATTERY_PERCENTAGE = 100

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(INITIAL_BATTERY_PERCENTAGE)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _userStatus = MutableStateFlow("Online") // Default status
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    fun updateLocation(location: Location?) {
        _userLocation.value = location
    }

    fun updateBatteryPercentage(percentage: Int) {
        _batteryPercentage.value = percentage.coerceIn(MIN_BATTERY_PERCENTAGE, MAX_BATTERY_PERCENTAGE)
    }

    fun updateUserStatus(status: String) {
        _userStatus.value = status
    }
}
