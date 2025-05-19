package com.familystalking.app.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(100)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _userStatus = MutableStateFlow("Driving")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    fun updateLocation(location: Location?) {
        _userLocation.value = location
    }

    fun updateBatteryPercentage(percentage: Int) {
        _batteryPercentage.value = percentage
    }

    fun updateUserStatus(status: String) {
        _userStatus.value = status
    }
}
