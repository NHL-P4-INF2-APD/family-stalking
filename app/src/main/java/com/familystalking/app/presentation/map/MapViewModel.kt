package com.familystalking.app.presentation.map

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familystalking.app.domain.model.FamilyMemberLocation
import com.familystalking.app.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import android.os.Build
import androidx.annotation.RequiresApi

data class MapScreenState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserLocation: com.familystalking.app.domain.model.Location? = null,
    val familyMemberLocations: List<FamilyMemberLocation> = emptyList()
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val fusedLocationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapScreenState())
    val uiState: StateFlow<MapScreenState> = _uiState.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                viewModelScope.launch {
                    updateLocation(location)
                }
            }
        }
    }

    init {
        //        startLocationUpdates() // Call from new method
        //        observeFamilyLocations() // Call from new method
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initializeLocationUpdates() {
        startLocationUpdates()
        observeFamilyLocations()
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    fun startLocationUpdates() {
        

        viewModelScope.launch {
            try {
                // Request last known location first
                val lastLocation = fusedLocationClient.lastLocation.await()
                lastLocation?.let { updateLocation(it) }

                // Start location updates
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    30000 // Update every 30 seconds
                ).build()

                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to get location updates: ${e.message}"
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateLocation(location: Location) {
        try {
            // Update location in Supabase
            locationRepository.updateUserLocation(
                latitude = location.latitude,
                longitude = location.longitude
            )

            // Update UI state
            _uiState.update { state ->
                state.copy(
                    currentUserLocation = com.familystalking.app.domain.model.Location(
                        id = UUID.randomUUID(),
                        userId = locationRepository.getAuthenticatedUserId() ?: throw IllegalStateException("User ID not found during location update"),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = Instant.now()
                    ),
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(
                    error = "Failed to update location: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun observeFamilyLocations() {
        viewModelScope.launch {
            try {
                // Observe location updates
                locationRepository.getFamilyMembersLocations().collect { locations ->
                    _uiState.update { state ->
                        state.copy(
                            familyMemberLocations = locations,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to get family member locations: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
} 