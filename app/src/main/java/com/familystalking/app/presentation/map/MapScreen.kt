package com.familystalking.app.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.bottomNavBar
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

@Composable
fun mapScreen(navController: NavController) {
    val context = LocalContext.current
    var batteryPercentage by remember { mutableStateOf(100) }
    var userStatus by remember { mutableStateOf("Driving") }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Simulated user location (fixed at Hoogenveen as shown in your screenshot)
    val userLocation = LatLng(52.75, 6.47) // Approximate location of Hoogenveen

    // Location permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionDialog = true
        }
    }

    // Check if we have location permission
    LaunchedEffect(Unit) {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            showPermissionDialog = true
        }
    }

    // Update battery percentage
    LaunchedEffect(Unit) {
        while (true) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryPercentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(10000) // Update every 10 seconds
        }
    }

    // Permission request dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location permission to show your position on the map.") },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPermissionDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Map with custom marker
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(userLocation, 8f)
            }

            // Map properties and UI settings
            val mapProperties = MapProperties(isMyLocationEnabled = false)
            val mapUiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = true
            )

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                // Custom marker at the user's location
                MarkerInfoWindow(
                    state = MarkerState(position = userLocation),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    visible = false // Hide the default marker
                ) {
                    // Custom marker content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Battery indicator
                        BatteryIndicator(batteryPercentage = batteryPercentage)

                        Spacer(modifier = Modifier.height(4.dp))

                        // Profile marker
                        ProfileMarker(initials = "OP")

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status indicator
                        StatusIndicator(status = userStatus)
                    }
                }
            }
        }

        // Bottom navigation
        bottomNavBar(
            currentRoute = "map",
            navController = navController
        )
    }
}

@Composable
fun BatteryIndicator(batteryPercentage: Int) {
    val batteryColor = when {
        batteryPercentage > 20 -> Color(0xFF4CAF50) // Green
        batteryPercentage > 10 -> Color(0xFFFFA000) // Orange
        else -> Color(0xFFF44336) // Red
    }

    Card(
        modifier = Modifier
            .width(70.dp)
            .height(24.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 8.dp)
                    .background(batteryColor, RoundedCornerShape(2.dp))
                    .border(0.5.dp, Color.DarkGray, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 4.dp)
                        .background(Color.DarkGray)
                        .align(Alignment.CenterEnd)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "$batteryPercentage%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(28.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ProfileMarker(initials: String) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Gray)
            .border(width = 2.dp, color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
