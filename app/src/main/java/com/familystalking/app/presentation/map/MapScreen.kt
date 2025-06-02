package com.familystalking.app.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.BottomNavBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

// Constants
private const val TAG = "MapScreen"
private const val DEFAULT_LATITUDE = 52.3676
private const val DEFAULT_LONGITUDE = 4.9041
private const val LOCATION_UPDATE_INTERVAL_MS = 5000L
private const val LOCATION_UPDATE_MIN_DISTANCE_M = 10f
private const val BATTERY_UPDATE_INTERVAL_MS = 30000L
private const val INITIAL_MAP_ZOOM = 15f
private const val MIN_USER_LOCATION_ZOOM = 15f
private const val DEFAULT_MAP_NO_LOCATION_ZOOM = 10f
private const val CAMERA_ANIMATION_DURATION_MS = 1000

private const val MARKER_ANCHOR_X = 0.5f
private const val MARKER_ANCHOR_Y = 0.8f
private const val BATTERY_INDICATOR_OFFSET_X_DP = -8

private const val BATTERY_LEVEL_FULL = 90
private const val BATTERY_LEVEL_VERY_GOOD = 75
private const val BATTERY_LEVEL_GOOD = 50
private const val BATTERY_LEVEL_OKAY = 25
private const val BATTERY_LEVEL_LOW = 10

private const val UI_MESSAGE_BACKGROUND_ALPHA = 0.7f
private const val CARD_BACKGROUND_ALPHA = 0.9f
private const val TEXT_PRIMARY_ALPHA = 0.87f

private const val MARKER_PROFILE_SIZE_DP_VALUE = 56
private val MARKER_PROFILE_SIZE_DP: Dp = MARKER_PROFILE_SIZE_DP_VALUE.dp
private const val MARKER_BORDER_WIDTH_DP_VALUE = 2
private val MARKER_BORDER_WIDTH_DP: Dp = MARKER_BORDER_WIDTH_DP_VALUE.dp
private const val MARKER_BATTERY_SPACER_HEIGHT_DP_VALUE = 28
private val MARKER_BATTERY_SPACER_HEIGHT_DP: Dp = MARKER_BATTERY_SPACER_HEIGHT_DP_VALUE.dp
private const val MARKER_STATUS_PADDING_TOP_DP_VALUE = 4
private val MARKER_STATUS_PADDING_TOP_DP: Dp = MARKER_STATUS_PADDING_TOP_DP_VALUE.dp
private const val MARKER_STATUS_PADDING_HORIZONTAL_DP_VALUE = 12
private val MARKER_STATUS_PADDING_HORIZONTAL_DP: Dp = MARKER_STATUS_PADDING_HORIZONTAL_DP_VALUE.dp
private const val MARKER_STATUS_PADDING_VERTICAL_DP_VALUE = 5
private val MARKER_STATUS_PADDING_VERTICAL_DP: Dp = MARKER_STATUS_PADDING_VERTICAL_DP_VALUE.dp
private const val MARKER_STATUS_BORDER_WIDTH_DP_VALUE = 1
private val MARKER_STATUS_BORDER_WIDTH_DP: Dp = MARKER_STATUS_BORDER_WIDTH_DP_VALUE.dp

private const val CARD_DEFAULT_ELEVATION_DP_VALUE = 2
private val CARD_DEFAULT_ELEVATION_DP: Dp = CARD_DEFAULT_ELEVATION_DP_VALUE.dp
private const val STATUS_INDICATOR_CORNER_RADIUS_PERCENT_VALUE = 50
private val STATUS_INDICATOR_SHAPE: Shape =
    RoundedCornerShape(STATUS_INDICATOR_CORNER_RADIUS_PERCENT_VALUE)
private val BATTERY_INDICATOR_SHAPE: Shape = RoundedCornerShape(20.dp)

private const val BATTERY_INDICATOR_PADDING_HORIZONTAL_DP_VALUE = 8
private val BATTERY_INDICATOR_PADDING_HORIZONTAL_DP: Dp =
    BATTERY_INDICATOR_PADDING_HORIZONTAL_DP_VALUE.dp
private const val BATTERY_INDICATOR_PADDING_VERTICAL_DP_VALUE = 4
private val BATTERY_INDICATOR_PADDING_VERTICAL_DP: Dp =
    BATTERY_INDICATOR_PADDING_VERTICAL_DP_VALUE.dp
private const val BATTERY_ICON_SIZE_DP_VALUE = 18
private val BATTERY_ICON_SIZE_DP: Dp = BATTERY_ICON_SIZE_DP_VALUE.dp
private const val BATTERY_INDICATOR_SPACING_DP_VALUE = 5
private val BATTERY_INDICATOR_SPACING_DP: Dp = BATTERY_INDICATOR_SPACING_DP_VALUE.dp

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val LOCATION_TRACKING_ACTIVE_MESSAGE = "Location tracking active"

@Composable
fun MapScreen( // Public Composable: PascalCase
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userLocationFromVm by viewModel.userLocation.collectAsStateWithLifecycle()
    val batteryPercentage by viewModel.batteryPercentage.collectAsStateWithLifecycle()
    val userStatus by viewModel.userStatus.collectAsStateWithLifecycle()

    var locationPermissionGrantedState by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showLocationDisabledAlert by remember { mutableStateOf(false) }
    var uiMessage by remember { mutableStateOf("Initializing Map...") }

    val defaultLocation = remember { LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE) }
    val currentMapLatLng: LatLng? = userLocationFromVm?.let { LatLng(it.latitude, it.longitude) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        locationPermissionGrantedState = isGranted
        if (isGranted) {
            uiMessage = "Permission granted. Fetching location..."
            Log.d(TAG, "Permission granted by user.")
        } else {
            uiMessage = "Location permission denied. Showing default location."
            Log.d(TAG, "Permission denied by user.")
            viewModel.updateLocation(null)
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestLocationPermissions( // Private non-composable helper: camelCase
            context = context,
            onPermissionsGranted = {
                locationPermissionGrantedState = true
                Log.d(TAG, "Permissions already granted.")
                uiMessage = "Permission OK. Checking location services..."
            },
            onShowRationale = { showPermissionRationaleDialog = true },
            onRequestPermissions = {
                uiMessage = "Requesting location permission..."
                locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
            }
        )
    }

    ManageLocationUpdatesEffect( // Private Composable: PascalCase
        locationPermissionGranted = locationPermissionGrantedState,
        onLocationUpdate = { viewModel.updateLocation(it) },
        onUiMessage = { uiMessage = it },
        onShowLocationDisabledAlert = { showLocationDisabledAlert = it }
    )

    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            viewModel.updateBatteryPercentage(
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            )
            delay(BATTERY_UPDATE_INTERVAL_MS)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        UiMessageBar(uiMessage) // Private Composable: PascalCase
        MapArea( // Private Composable: PascalCase
            modifier = Modifier.weight(1f),
            currentMapLatLng = currentMapLatLng,
            defaultLocation = defaultLocation,
            batteryPercentage = batteryPercentage,
            userStatus = userStatus
        )
        BottomNavBar(navController = navController, currentRoute = "map")
    }

    if (showPermissionRationaleDialog) {
        LocationPermissionRationaleDialog( // Public Composable: PascalCase
            onConfirm = {
                showPermissionRationaleDialog = false
                locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
            },
            onDismiss = {
                showPermissionRationaleDialog = false
                uiMessage = "Permission rationale dismissed. Default location shown."
                viewModel.updateLocation(null)
            }
        )
    }

    if (showLocationDisabledAlert) {
        LocationServicesDisabledDialog( // Public Composable: PascalCase
            context = context,
            onDismiss = { showLocationDisabledAlert = false }
        )
    }
}

// Private non-composable helper function: camelCase
private fun checkAndRequestLocationPermissions(
    context: Context,
    onPermissionsGranted: () -> Unit,
    onShowRationale: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val fineLocationGranted = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocationGranted = ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (fineLocationGranted || coarseLocationGranted) {
        onPermissionsGranted()
    } else {
        Log.d(TAG, "Permissions not granted.")
        val activity = context as? Activity
        val shouldShowFineRationale = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) ?: false
        val shouldShowCoarseRationale = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) ?: false

        if (shouldShowFineRationale || shouldShowCoarseRationale) {
            onShowRationale()
        } else {
            onRequestPermissions()
        }
    }
}

@Composable
private fun UiMessageBar(message: String) { // Private Composable: PascalCase
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiaryContainer.copy(
                    alpha = UI_MESSAGE_BACKGROUND_ALPHA
                )
            )
            .padding(8.dp),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onTertiaryContainer
    )
}

@Composable
private fun MapArea( // Private Composable: PascalCase
    modifier: Modifier = Modifier,
    currentMapLatLng: LatLng?,
    defaultLocation: LatLng,
    batteryPercentage: Int,
    userStatus: String
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                currentMapLatLng ?: defaultLocation, INITIAL_MAP_ZOOM
            )
        }
        MapCameraAnimator(
            cameraPositionState,
            currentMapLatLng,
            defaultLocation
        ) // Private Composable: PascalCase
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = false
            )
        ) {
            currentMapLatLng?.let { validLatLng ->
                UserMarker( // Private Composable: PascalCase
                    position = validLatLng,
                    batteryPercentage = batteryPercentage,
                    userStatus = userStatus,
                    initials = "OP" // Replace "OP" with dynamic initials
                )
            }
        }
    }
}

@Composable
private fun MapCameraAnimator( // Private Composable: PascalCase
    cameraPositionState: CameraPositionState,
    currentMapLatLng: LatLng?,
    defaultLocation: LatLng
) {
    LaunchedEffect(currentMapLatLng, defaultLocation) {
        val targetLatLng = currentMapLatLng ?: defaultLocation
        val targetZoom: Float
        val needsAnimation: Boolean

        if (currentMapLatLng != null) {
            targetZoom = cameraPositionState.position.zoom.coerceAtLeast(MIN_USER_LOCATION_ZOOM)
            needsAnimation = cameraPositionState.position.target != targetLatLng ||
                    cameraPositionState.position.zoom < MIN_USER_LOCATION_ZOOM
        } else {
            targetZoom = DEFAULT_MAP_NO_LOCATION_ZOOM
            needsAnimation = cameraPositionState.position.target != targetLatLng ||
                    cameraPositionState.position.zoom != DEFAULT_MAP_NO_LOCATION_ZOOM
        }

        if (needsAnimation) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(targetLatLng, targetZoom),
                CAMERA_ANIMATION_DURATION_MS
            )
        }
    }
}

@Composable
private fun UserMarker( // Private Composable: PascalCase
    position: LatLng,
    batteryPercentage: Int,
    userStatus: String,
    initials: String
) {
    MarkerComposable(
        keys = arrayOf(batteryPercentage, userStatus, position),
        state = MarkerState(position = position),
        anchor = Offset(MARKER_ANCHOR_X, MARKER_ANCHOR_Y)
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(MARKER_BATTERY_SPACER_HEIGHT_DP))
                ProfileMarker(initials = initials) // Public Composable: PascalCase
                StatusIndicator(status = userStatus) // Public Composable: PascalCase
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = BATTERY_INDICATOR_OFFSET_X_DP.dp)
            ) {
                BatteryIndicator(batteryPercentage = batteryPercentage) // Public Composable: PascalCase
            }
        }
    }
}

private data class LocationUpdateParams(
    val context: Context,
    val locationManager: LocationManager,
    val listener: LocationListener,
    val isGpsEnabled: Boolean,
    val isNetworkEnabled: Boolean,
    val onLocationUpdate: (Location?) -> Unit,
    val onUiMessage: (String) -> Unit
)

@SuppressLint("MissingPermission")
@Composable
private fun ManageLocationUpdatesEffect( // Private Composable: PascalCase
    locationPermissionGranted: Boolean,
    onLocationUpdate: (Location?) -> Unit,
    onUiMessage: (String) -> Unit,
    onShowLocationDisabledAlert: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, locationPermissionGranted) {
        if (!locationPermissionGranted) {
            onLocationUpdate(null)
            onDispose {
                Log.d(
                    TAG,
                    "ManageLocationUpdatesEffect: No permissions, no cleanup needed."
                )
            }
        } else {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                onShowLocationDisabledAlert(true)
                onLocationUpdate(null)
                onUiMessage("Enable Location Services for live tracking.")
                onDispose {
                    Log.d(
                        TAG,
                        "ManageLocationUpdatesEffect: Location services disabled, no cleanup needed."
                    )
                }
            } else {
                onShowLocationDisabledAlert(false)

                val locationListener =
                    createLocationListener( // Private non-composable helper: camelCase
                        context = context,
                        onLocationUpdate = onLocationUpdate,
                        onUiMessage = onUiMessage,
                        onShowLocationDisabledAlert = onShowLocationDisabledAlert,
                        onReCheckAfterProviderEnabled = {
                            Log.d(TAG, "A provider was re-enabled.")
                        }
                    )
                val params = LocationUpdateParams(
                    context = context,
                    locationManager = locationManager,
                    listener = locationListener,
                    isGpsEnabled = isGpsEnabled,
                    isNetworkEnabled = isNetworkEnabled,
                    onLocationUpdate = onLocationUpdate,
                    onUiMessage = onUiMessage
                )
                try {
                    requestLastKnownAndUpdates(params) // Private non-composable helper: camelCase
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during location setup: ${e.message}", e)
                    onLocationUpdate(null)
                    onUiMessage("Location permission error.")
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "IllegalArgumentException during location setup: ${e.message}", e)
                    onLocationUpdate(null)
                    onUiMessage("Configuration error for location updates.")
                }
                onDispose {
                    Log.d(TAG, "Removing location updates from listener.")
                    try {
                        locationManager.removeUpdates(locationListener)
                    } catch (secEx: SecurityException) {
                        Log.e(
                            TAG,
                            "SecurityException removing location updates: ${secEx.message}",
                            secEx
                        )
                    } catch (argEx: IllegalArgumentException) {
                        Log.e(
                            TAG,
                            "IllegalArgumentException removing location updates: ${argEx.message}",
                            argEx
                        )
                    }
                }
            }
        }
    }
}

// Private non-composable helper function: camelCase
private fun createLocationListener(
    context: Context,
    onLocationUpdate: (Location?) -> Unit,
    onUiMessage: (String) -> Unit,
    onShowLocationDisabledAlert: (Boolean) -> Unit,
    onReCheckAfterProviderEnabled: () -> Unit
): LocationListener {
    return object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "Location changed (lat/lng): ${location.latitude}, ${location.longitude}")
            onLocationUpdate(location)
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Provider enabled: $provider")
            onUiMessage("$provider enabled. Re-checking services...")
            onReCheckAfterProviderEnabled()
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Provider disabled: $provider")
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            ) {
                onShowLocationDisabledAlert(true)
                onLocationUpdate(null)
                onUiMessage("All location services disabled.")
            } else {
                onUiMessage("$provider disabled. Other providers might be active.")
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "Provider $provider status changed to $status")
        }
    }
}

@SuppressLint("MissingPermission")
// Private non-composable helper function: camelCase
private fun requestLastKnownAndUpdates(params: LocationUpdateParams) {
    var lastKnownLoc: Location? = null
    if (params.isGpsEnabled) {
        lastKnownLoc = params.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    }
    if (lastKnownLoc == null && params.isNetworkEnabled) {
        lastKnownLoc = params.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }

    lastKnownLoc?.let {
        params.onLocationUpdate(it)
        params.onUiMessage(LOCATION_TRACKING_ACTIVE_MESSAGE)
    } ?: params.onUiMessage("No last known location. Waiting for new updates...")


    if (params.isGpsEnabled) {
        params.locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            LOCATION_UPDATE_INTERVAL_MS,
            LOCATION_UPDATE_MIN_DISTANCE_M,
            params.listener
        )
        Log.d(TAG, "Requested GPS location updates.")
    }
    if (params.isNetworkEnabled) {
        params.locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            LOCATION_UPDATE_INTERVAL_MS,
            LOCATION_UPDATE_MIN_DISTANCE_M,
            params.listener
        )
        Log.d(TAG, "Requested Network location updates.")
    }
}

@Composable
fun LocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) { // Public Composable: PascalCase
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Permission Needed") },
        text = { Text("This app uses your location to show it on the map. Please grant the permission for the best experience.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Grant") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LocationServicesDisabledDialog(
    context: Context,
    onDismiss: () -> Unit
) { // Public Composable: PascalCase
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Services Disabled") },
        text = { Text("Please enable GPS or Network location services for live location updates.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }) { Text("Open Settings") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Dismiss") } }
    )
}

@Composable
fun BatteryIndicator(batteryPercentage: Int) { // Public Composable: PascalCase
    val batteryColor = when {
        batteryPercentage > BATTERY_LEVEL_GOOD -> Color(0xFF4CAF50)
        batteryPercentage > BATTERY_LEVEL_OKAY -> Color(0xFFFFA000)
        else -> Color(0xFFD32F2F)
    }
    Card(
        shape = BATTERY_INDICATOR_SHAPE,
        elevation = CardDefaults.cardElevation(
            defaultElevation = CARD_DEFAULT_ELEVATION_DP
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = CARD_BACKGROUND_ALPHA)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = BATTERY_INDICATOR_PADDING_HORIZONTAL_DP,
                vertical = BATTERY_INDICATOR_PADDING_VERTICAL_DP
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BATTERY_INDICATOR_SPACING_DP)
        ) {
            val icon = when {
                batteryPercentage > BATTERY_LEVEL_FULL -> Icons.Filled.BatteryFull
                batteryPercentage > BATTERY_LEVEL_VERY_GOOD -> Icons.Filled.Battery5Bar
                batteryPercentage > BATTERY_LEVEL_GOOD -> Icons.Filled.Battery3Bar
                batteryPercentage > BATTERY_LEVEL_OKAY -> Icons.Filled.Battery2Bar
                batteryPercentage > BATTERY_LEVEL_LOW -> Icons.Filled.Battery1Bar
                else -> Icons.Filled.BatteryAlert
            }
            Icon(
                imageVector = icon,
                contentDescription = "Battery Level",
                tint = batteryColor,
                modifier = Modifier.size(BATTERY_ICON_SIZE_DP)
            )
            Text(
                text = "$batteryPercentage%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = TEXT_PRIMARY_ALPHA)
            )
        }
    }
}

@Composable
fun StatusIndicator(status: String) { // Public Composable: PascalCase
    Card(
        modifier = Modifier.padding(top = MARKER_STATUS_PADDING_TOP_DP),
        shape = STATUS_INDICATOR_SHAPE,
        elevation = CardDefaults.cardElevation(
            defaultElevation = CARD_DEFAULT_ELEVATION_DP
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = CARD_BACKGROUND_ALPHA)
        ),
        border = BorderStroke(
            MARKER_STATUS_BORDER_WIDTH_DP,
            Color.Black.copy(alpha = UI_MESSAGE_BACKGROUND_ALPHA)
        )
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = MARKER_STATUS_PADDING_HORIZONTAL_DP,
                vertical = MARKER_STATUS_PADDING_VERTICAL_DP
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black.copy(alpha = TEXT_PRIMARY_ALPHA)
            )
        }
    }
}

@Composable
fun ProfileMarker(initials: String) { // Public Composable: PascalCase
    Box(
        modifier = Modifier
            .size(MARKER_PROFILE_SIZE_DP)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                width = MARKER_BORDER_WIDTH_DP,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}