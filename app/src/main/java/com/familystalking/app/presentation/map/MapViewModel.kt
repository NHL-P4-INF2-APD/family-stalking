package com.familystalking.app.presentation.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_BATTERY_PERCENTAGE = 0
private const val MAX_BATTERY_PERCENTAGE = 100
private const val INITIAL_BATTERY_PERCENTAGE = 100

@HiltViewModel
class MapViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(INITIAL_BATTERY_PERCENTAGE)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _userStatus = MutableStateFlow("Online")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    val shouldShowBatteryOnMap: StateFlow<Boolean> = settingsDataStore.showBatteryPercentagePreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = true
        )

    val isLocationSharingPreferred: StateFlow<Boolean> = settingsDataStore.locationSharingPreference
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = true
        )

    fun updateLocation(location: Location?) {
        _userLocation.value = location // Always update local display for the user
    }

    fun updateBatteryPercentage(percentage: Int) {
        _batteryPercentage.value = percentage.coerceIn(MIN_BATTERY_PERCENTAGE, MAX_BATTERY_PERCENTAGE)
        // TODO: Optionally send to backend if sharing is on
    }

    fun updateUserStatus(status: String) {
        _userStatus.value = status
        // TODO: Optionally send to backend if sharing is on
    }
}