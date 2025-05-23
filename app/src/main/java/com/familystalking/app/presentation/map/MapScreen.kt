package com.familystalking.app.presentation.map

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.bottomNavBar
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.familystalking.app.presentation.navigation.Screen

private const val DEFAULT_LATITUDE = 52.3676
private const val DEFAULT_LONGITUDE = 4.9041
private const val DEFAULT_ZOOM = 12f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                viewModel.initializeLocationUpdates()
            } else {
                // Handle cases for API < 26 if necessary, or show an error/message
                // For now, we'll do nothing if API level is too low for these specific features
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionState.status.isGranted
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    mapToolbarEnabled = true
                )
            ) {
                uiState.familyMemberLocations.forEach { memberLocation ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                memberLocation.location.latitude,
                                memberLocation.location.longitude
                            )
                        ),
                        title = memberLocation.name,
                        snippet = "Last seen: ${memberLocation.location.timestamp}"
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        bottomNavBar(
            currentRoute = Screen.Map.route,
            navController = navController
        )
    }
}
