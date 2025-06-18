package com.familystalking.app.presentation.map

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.data.datastore.SettingsDataStore
import com.familystalking.app.domain.model.FamilyMemberLocation
import com.familystalking.app.domain.repository.FamilyRepository
import com.familystalking.app.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MIN_BATTERY_PERCENTAGE = 0
private const val MAX_BATTERY_PERCENTAGE = 100
private const val INITIAL_BATTERY_PERCENTAGE = 100
private const val TAG = "MapViewModel"

@HiltViewModel
class MapViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val locationRepository: LocationRepository,
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _batteryPercentage = MutableStateFlow(INITIAL_BATTERY_PERCENTAGE)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _userStatus = MutableStateFlow("Online")
    val userStatus: StateFlow<String> = _userStatus.asStateFlow()

    private val _friendLocations = MutableStateFlow<List<FamilyMemberLocation>>(emptyList())
    val friendLocations: StateFlow<List<FamilyMemberLocation>> = _friendLocations.asStateFlow()

    private val _isLoadingFriends = MutableStateFlow(false)
    val isLoadingFriends: StateFlow<Boolean> = _isLoadingFriends.asStateFlow()

    private val _currentUserName = MutableStateFlow<String>("User")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

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

    init {
        // Start collecting friend locations with real-time updates
        observeFriendLocations()
        // Get current user's name
        getCurrentUser()
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            try {
                val currentUser = familyRepository.getCurrentUser()
                _currentUserName.value = currentUser.name
                Log.d(TAG, "Current user name: ${currentUser.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current user", e)
                _currentUserName.value = "User"
            }
        }
    }

    private fun observeFriendLocations() {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                _isLoadingFriends.value = true
                locationRepository.getFamilyMembersLocations()
                    .catch { e ->
                        Log.e(TAG, "Error observing friend locations", e)
                        _friendLocations.value = emptyList()
                        _isLoadingFriends.value = false
                    }
                    .collect { locations ->
                        Log.d(TAG, "Received ${locations.size} friend locations")
                        _friendLocations.value = locations
                        _isLoadingFriends.value = false
                    }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateLocation(location: Location?) {
        _userLocation.value = location // Always update local display for the user
        
        // Share location to backend if sharing is enabled and we have a valid location
        location?.let { validLocation ->
            viewModelScope.launch {
                if (isLocationSharingPreferred.value) {
                    try {
                        locationRepository.updateUserLocation(
                            latitude = validLocation.latitude,
                            longitude = validLocation.longitude
                        )
                        Log.d(TAG, "User location shared to backend")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to share location to backend", e)
                    }
                }
            }
        }
    }

    fun updateBatteryPercentage(percentage: Int) {
        _batteryPercentage.value = percentage.coerceIn(MIN_BATTERY_PERCENTAGE, MAX_BATTERY_PERCENTAGE)
    }

    fun updateUserStatus(status: String) {
        _userStatus.value = status
    }

    /**
     * Manually refresh friend locations
     */
    fun refreshFriendLocations() {
        observeFriendLocations()
    }

    /**
     * Generate initials based on a person's name
     * Rules:
     * - If first and last name: use first letter of each
     * - If has middle name: use first and last (skip middle)
     * - If only first name: use first 2 letters of first name
     * - Fallback: "?" for empty/invalid names
     */
    fun getInitials(fullName: String): String {
        val cleanName = fullName.trim()
        if (cleanName.isEmpty()) return "?"

        val nameParts = cleanName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        return when (nameParts.size) {
            0 -> "?"
            1 -> {
                // Only first name - use first 2 letters
                val firstName = nameParts[0]
                if (firstName.length >= 2) {
                    firstName.take(2).uppercase()
                } else {
                    firstName.uppercase().padEnd(2, '?')
                }
            }
            2 -> {
                // First and last name
                "${nameParts[0].first().uppercaseChar()}${nameParts[1].first().uppercaseChar()}"
            }
            else -> {
                // Has middle name(s) - use first and last
                "${nameParts.first().first().uppercaseChar()}${nameParts.last().first().uppercaseChar()}"
            }
        }
    }

    /**
     * Get initials for the current user
     */
    fun getCurrentUserInitials(): String {
        return getInitials(currentUserName.value)
    }

    /**
     * Get initials for a friend (backward compatibility)
     */
    fun getFriendInitials(friendName: String): String {
        return getInitials(friendName)
    }
}