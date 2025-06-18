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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

private const val MIN_BATTERY_PERCENTAGE = 0
private const val MAX_BATTERY_PERCENTAGE = 100
private const val INITIAL_BATTERY_PERCENTAGE = 100
private const val LOCATION_SHARING_INTERVAL_MS = 5000L // 5 seconds
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

    private var locationSharingJob: Job? = null

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
        Log.d(TAG, "🚀 MapViewModel initialized")
        
        // Start collecting friend locations with real-time updates
        observeFriendLocations()
        // Get current user's name
        getCurrentUser()
        // Start periodic location sharing
        startLocationSharingJob()
        
        // Log initial state
        viewModelScope.launch {
            delay(1000) // Wait a moment for initial values
            Log.d(TAG, "📊 Initial state - Location sharing enabled: ${isLocationSharingPreferred.value}, User location: ${_userLocation.value}")
        }
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
        if (location != null) {
            Log.d(TAG, "📍 Location updated: lat=${location.latitude}, lng=${location.longitude}, provider=${location.provider}, accuracy=${location.accuracy}m, time=${location.time}")
        } else {
            Log.w(TAG, "⚠️ Location updated with NULL value")
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
     * Manually test location sharing (for debugging)
     */
    fun testLocationSharing() {
        Log.d(TAG, "🧪 Manual location sharing test triggered")
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                shareCurrentLocationToBackend()
            } else {
                Log.w(TAG, "Cannot test location sharing - requires Android O or higher")
            }
        }
    }

    /**
     * Start periodic location sharing job
     */
    private fun startLocationSharingJob() {
        locationSharingJob?.cancel() // Cancel any existing job
        locationSharingJob = viewModelScope.launch {
            Log.d(TAG, "Location sharing job coroutine started")
            
            // First, wait a bit for location to be obtained
            delay(2000) // Wait 2 seconds before first attempt
            
            while (true) {
                try {
                    Log.d(TAG, "Location sharing job iteration - Android version: ${Build.VERSION.SDK_INT}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        shareCurrentLocationToBackend()
                    } else {
                        Log.w(TAG, "Location sharing requires Android O or higher")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in location sharing job", e)
                }
                
                delay(LOCATION_SHARING_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Location sharing job started - will share location every ${LOCATION_SHARING_INTERVAL_MS / 1000} seconds")
    }

    /**
     * Share current location to backend if sharing is enabled
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun shareCurrentLocationToBackend() {
        val currentLocation = _userLocation.value
        val isSharingEnabled = isLocationSharingPreferred.value
        
        Log.d(TAG, "=== LOCATION SHARING ATTEMPT ===")
        Log.d(TAG, "Location available: ${currentLocation != null}")
        if (currentLocation != null) {
            Log.d(TAG, "Location details: lat=${currentLocation.latitude}, lng=${currentLocation.longitude}, provider=${currentLocation.provider}, accuracy=${currentLocation.accuracy}")
        }
        Log.d(TAG, "Sharing enabled: $isSharingEnabled")
        
        if (isSharingEnabled && currentLocation != null) {
            try {
                Log.d(TAG, "Calling locationRepository.updateUserLocation...")
                locationRepository.updateUserLocation(
                    latitude = currentLocation.latitude,
                    longitude = currentLocation.longitude
                )
                Log.d(TAG, "✅ Location shared successfully to backend: ${currentLocation.latitude}, ${currentLocation.longitude}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to share location to backend", e)
            }
        } else {
            if (!isSharingEnabled) {
                Log.w(TAG, "⚠️ Location sharing is disabled in settings")
            }
            if (currentLocation == null) {
                Log.w(TAG, "⚠️ No current location available to share")
            }
        }
        Log.d(TAG, "=== END LOCATION SHARING ATTEMPT ===")
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

    override fun onCleared() {
        super.onCleared()
        locationSharingJob?.cancel()
        Log.d(TAG, "MapViewModel cleared - location sharing job cancelled")
    }
}