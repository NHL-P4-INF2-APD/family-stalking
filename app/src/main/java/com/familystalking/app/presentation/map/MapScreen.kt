package com.familystalking.app.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.bottomNavBar
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState

private const val DEFAULT_LATITUDE = 52.3676
private const val DEFAULT_LONGITUDE = 4.9041
private const val DEFAULT_ZOOM = 12f

@Composable
fun mapScreen(navController: NavController) {
    val amsterdam = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(amsterdam, DEFAULT_ZOOM)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = com.google.maps.android.compose.MarkerState(position = amsterdam),
                    title = "Amsterdam",
                    snippet = "Marker in Amsterdam"
                )
            }
        }
        bottomNavBar(
            currentRoute = "map",
            navController = navController
        )
    }
}
