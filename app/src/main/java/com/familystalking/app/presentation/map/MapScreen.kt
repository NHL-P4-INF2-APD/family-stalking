package com.familystalking.app.presentation.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.bottomNavBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private const val TAG = "MapScreen"

@Composable
fun mapScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var batteryPercentage by remember { mutableStateOf(100) }
    var userStatus by remember { mutableStateOf("Driving") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var debugMessage by remember { mutableStateOf("Waiting for location...") }

    // Default location (Hoogenveen) as fallback
    val defaultLocation = LatLng(52.75, 6.47)

    // Location permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        locationPermissionGranted = fineLocationGranted || coarseLocationGranted

        if (locationPermissionGranted) {
            Log.d(TAG, "Permission granted, starting location updates")
            // Permission granted, location updates will start in the DisposableEffect
        } else {
            Log.d(TAG, "Permission denied")
            showPermissionDialog = true
        }
    }

    // Check if we have location permission
    LaunchedEffect(Unit) {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        locationPermissionGranted = fineLocationPermission || coarseLocationPermission

        if (!locationPermissionGranted) {
            Log.d(TAG, "Need to request permission")
            showPermissionDialog = true
        } else {
            Log.d(TAG, "Permission already granted")
        }
    }

    // Location tracking using Android's LocationManager
    DisposableEffect(lifecycleOwner, locationPermissionGranted) {
        var locationListener: LocationListener? = null
        var locationManager: LocationManager? = null

        if (locationPermissionGranted) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Check if GPS or network provider is enabled
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled || isNetworkEnabled) {
                // Create location listener
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.d(TAG, "Location update: ${location.latitude}, ${location.longitude}")
                        userLocation = LatLng(location.latitude, location.longitude)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                // Try to get last known location
                try {
                    if (isGpsEnabled) {
                        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (lastKnownLocation != null) {
                            Log.d(TAG, "Last known GPS location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                            userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        }
                    } else if (isNetworkEnabled) {
                        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (lastKnownLocation != null) {
                            Log.d(TAG, "Last known network location: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                            userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error getting last location: ${e.message}")
                }

                // Request location updates
                try {
                    if (isGpsEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            5000, // 5 seconds
                            10f,  // 10 meters
                            locationListener
                        )
                        Log.d(TAG, "Started GPS location updates")
                    } else if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            5000, // 5 seconds
                            10f,  // 10 meters
                            locationListener
                        )
                        Log.d(TAG, "Started network location updates")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Error requesting location updates: ${e.message}")
                }
            } else {
                Log.d(TAG, "No location provider enabled")
                // Use default location if no provider is enabled
                userLocation = defaultLocation
            }
        } else {
            Log.d(TAG, "No location permission")
            // Use default location if no permission
            userLocation = defaultLocation
        }

        // Remove location updates when the composable is disposed
        onDispose {
            if (locationListener != null && locationManager != null) {
                locationManager.removeUpdates(locationListener)
                Log.d(TAG, "Location updates stopped")
            }
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
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    userLocation = defaultLocation
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
            val currentLocation = userLocation ?: defaultLocation
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
            }

            // Update camera position when location changes
            LaunchedEffect(userLocation) {
                userLocation?.let { location ->
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(location, 15f)
                    )
                }
            }

            // Map properties and UI settings
            val mapProperties = MapProperties(
                isMyLocationEnabled = false // We'll use our custom marker instead
            )
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
                // Only show marker if we have a location
                userLocation?.let { location ->
                    MarkerInfoWindow(
                        state = MarkerState(position = location),
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
