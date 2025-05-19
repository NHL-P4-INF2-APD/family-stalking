package com.familystalking.app.presentation.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.bottomNavBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay

private const val TAG = "MapScreen"

@Composable
fun mapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val userLocationFromVm by viewModel.userLocation.collectAsStateWithLifecycle()
    val batteryPercentage by viewModel.batteryPercentage.collectAsStateWithLifecycle()
    val userStatus by viewModel.userStatus.collectAsStateWithLifecycle()

    val currentMapLatLng: LatLng? = userLocationFromVm?.let { LatLng(it.latitude, it.longitude) }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var showLocationDisabledAlert by remember { mutableStateOf(false) }
    var uiMessage by remember { mutableStateOf("Initializing Map...") }

    val defaultLocation = LatLng(52.3676, 4.9041)

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationPermissionGranted) {
            uiMessage = "Permission granted. Fetching location..."
            Log.d(TAG, "Permission granted by user.")
        } else {
            uiMessage = "Location permission denied. Showing default location."
            Log.d(TAG, "Permission denied by user.")
            viewModel.updateLocation(null)
        }
    }

    fun checkAndRequestPermissions() {
        locationPermissionGranted = ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!locationPermissionGranted) {
            Log.d(TAG, "Permissions not granted. Requesting.")
            locationPermissionLauncher.launch(locationPermissions)
            uiMessage = "Requesting location permission..."
        } else {
            Log.d(TAG, "Permissions already granted.")
            uiMessage = "Permission OK. Checking location services..."
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions()
    }

    DisposableEffect(lifecycleOwner, locationPermissionGranted) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var locationListener: LocationListener? = null

        if (locationPermissionGranted) {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                showLocationDisabledAlert = true
                viewModel.updateLocation(null)
                uiMessage = "Enable Location Services for live tracking."
            } else {
                showLocationDisabledAlert = false
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        viewModel.updateLocation(location)
                        uiMessage = "Location: ${location.latitude}, ${location.longitude}"
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) { checkAndRequestPermissions() }
                    override fun onProviderDisabled(provider: String) {
                        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            showLocationDisabledAlert = true; viewModel.updateLocation(null)
                        }
                    }
                }
                try {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        var lastKnownLoc: Location? = null
                        if (isGpsEnabled) lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (lastKnownLoc == null && isNetworkEnabled) lastKnownLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        lastKnownLoc?.let { viewModel.updateLocation(it); uiMessage = "Using last known location." } ?: run { uiMessage = "No last known location. Waiting..." }

                        val minTimeMs = 5000L; val minDistanceM = 10f
                        if (isGpsEnabled) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistanceM, locationListener)
                        if (isNetworkEnabled) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, minDistanceM, locationListener)
                        if (!isGpsEnabled && !isNetworkEnabled) { viewModel.updateLocation(null); uiMessage = "No location providers."}
                    } else { viewModel.updateLocation(null) }
                } catch (e: SecurityException) {
                    locationPermissionGranted = false; viewModel.updateLocation(null); uiMessage = "Location permission error."
                }
            }
        } else {
            viewModel.updateLocation(null); uiMessage = "Permission denied. Using default."
        }
        onDispose { locationListener?.let { try { locationManager.removeUpdates(it) } catch (ex: Exception) { Log.e(TAG, "Error removing updates: ${ex.message}") } } }
    }

    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            viewModel.updateBatteryPercentage(batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            delay(30000)
        }
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = { Text("Location Permission Needed") },
            text = { Text("This app uses your location to show it on the map. Please grant the permission.") },
            confirmButton = { Button(onClick = { showPermissionRationaleDialog = false; locationPermissionLauncher.launch(locationPermissions) }) { Text("Grant") } },
            dismissButton = { Button(onClick = { showPermissionRationaleDialog = false }) { Text("Cancel") } }
        )
    }

    if (showLocationDisabledAlert) {
        AlertDialog(
            onDismissRequest = { showLocationDisabledAlert = false },
            title = { Text("Location Services Disabled") },
            text = { Text("Please enable GPS or Network location services for live location updates.") },
            confirmButton = { Button(onClick = { showLocationDisabledAlert = false; context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Open Settings") } },
            dismissButton = { Button(onClick = { showLocationDisabledAlert = false }) { Text("Dismiss") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = uiMessage,
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)).padding(8.dp),
            textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(currentMapLatLng ?: defaultLocation, 15f) }
            LaunchedEffect(currentMapLatLng) {
                currentMapLatLng?.let {
                    if (cameraPositionState.position.target != it || cameraPositionState.position.zoom < 14f) {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, cameraPositionState.position.zoom.coerceAtLeast(15f)), 1000)
                    }
                } ?: run {
                    if (cameraPositionState.position.target != defaultLocation) {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f), 1000)
                    }
                }
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = true)
            ) {
                currentMapLatLng?.let { validLatLng ->
                    MarkerComposable(
                        keys = arrayOf(batteryPercentage, userStatus, validLatLng),
                        state = MarkerState(position = validLatLng),
                        anchor = Offset(0.5f, 0.8f)
                    ) {
                        Box {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(28.dp))
                                ProfileMarker(initials = "OP")
                                StatusIndicator(status = userStatus)
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = (-8).dp, y = 0.dp)
                            ) {
                                BatteryIndicatorSimple(batteryPercentage = batteryPercentage)
                            }
                        }
                    }
                }
            }
        }
        bottomNavBar(currentRoute = "map", navController = navController)
    }
}

@Composable
fun BatteryIndicatorSimple(batteryPercentage: Int) {
    val batteryColor = when {
        batteryPercentage > 50 -> Color(0xFF4CAF50)
        batteryPercentage > 20 -> Color(0xFFFFA000)
        else -> Color(0xFFD32F2F)
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val icon = when {
                batteryPercentage > 90 -> Icons.Filled.BatteryFull
                batteryPercentage > 75 -> Icons.Filled.Battery5Bar // Changed for more granularity
                batteryPercentage > 50 -> Icons.Filled.Battery3Bar // Changed for more granularity
                batteryPercentage > 25 -> Icons.Filled.Battery2Bar
                batteryPercentage > 10 -> Icons.Filled.Battery1Bar // Changed for more granularity
                else -> Icons.Filled.BatteryAlert
            }
            Icon(
                imageVector = icon,
                contentDescription = "Battery Level",
                tint = batteryColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "$batteryPercentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.87f)
            )
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    Card(
        modifier = Modifier.padding(top = 4.dp),
        shape = RoundedCornerShape(50),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.7f))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = 0.87f)
            )
        }
    }
}

@Composable
fun ProfileMarker(initials: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .border(width = 2.dp, color = Color.White, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal
        )
    }
}