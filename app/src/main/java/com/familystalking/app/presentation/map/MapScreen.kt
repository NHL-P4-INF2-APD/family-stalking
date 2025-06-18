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
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
private const val BATTERY_INDICATOR_INTERNAL_SPACING_DP_VALUE = 4
private val BATTERY_INDICATOR_INTERNAL_SPACING_DP: Dp = BATTERY_INDICATOR_INTERNAL_SPACING_DP_VALUE.dp

private val SHARING_ICON_SIZE_DP: Dp = 18.dp
private val MARKER_ELEMENT_SPACING_DP: Dp = 4.dp

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private const val MSG_PERM_DENIED = "Location permission denied."
private const val MSG_PERM_RATIONALE_DISMISSED = "Permission rationale dismissed."
private const val MSG_REQUESTING_PERM = "Requesting location permission..."
private const val MSG_DEVICE_LOCATION_OFF = "Enable Device Location Services for map features."
private const val MSG_LOCATION_ERROR = "Location error."
private const val MSG_LOCATION_CONFIG_ERROR = "Location configuration error."
// private const val MSG_SHARING_OFF_SETTINGS = "Location sharing is off in settings." // No longer shown in UiMessageBar
private const val MSG_PROVIDER_RE_ENABLED = "Location provider re-enabled."
private const val MSG_PROVIDER_DISABLED_ALL_OFF = "All device location services disabled."
private const val MSG_PROVIDER_DISABLED_SOME_OFF = "A location provider was disabled."


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userLocationFromVm by viewModel.userLocation.collectAsStateWithLifecycle()
    val batteryPercentage by viewModel.batteryPercentage.collectAsStateWithLifecycle()
    val userStatus by viewModel.userStatus.collectAsStateWithLifecycle()
    val shouldShowBatteryOnMap by viewModel.shouldShowBatteryOnMap.collectAsStateWithLifecycle()
    val isLocationSharingPreferred by viewModel.isLocationSharingPreferred.collectAsStateWithLifecycle()
    val friendLocations by viewModel.friendLocations.collectAsStateWithLifecycle()
    val isLoadingFriends by viewModel.isLoadingFriends.collectAsStateWithLifecycle()

    var locationPermissionGrantedState by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var showLocationDisabledAlert by remember { mutableStateOf(false) }
    var uiMessage by remember { mutableStateOf("") }

    val defaultLocation = remember { LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE) }
    // currentMapLatLng is always based on userLocationFromVm for the user's own map display
    val currentMapLatLng: LatLng? = userLocationFromVm?.let { LatLng(it.latitude, it.longitude) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGrantedState = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (!locationPermissionGrantedState) {
            uiMessage = MSG_PERM_DENIED
            viewModel.updateLocation(null) // Clear location in VM if permission denied
        } else {
            uiMessage = "" // Clear message if granted
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGrantedState) {
            checkAndRequestLocationPermissions(
                context = context,
                onPermissionsGranted = {
                    locationPermissionGrantedState = true
                    uiMessage = ""
                },
                onShowRationale = { showPermissionRationaleDialog = true },
                onRequestPermissions = {
                    uiMessage = MSG_REQUESTING_PERM
                    locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
                }
            )
        }
    }

    ManageLocationUpdatesEffect(
        locationPermissionGranted = locationPermissionGrantedState,
        // isLocationSharingPreferredSetting is NO LONGER passed here.
        // The effect's job is to get location if permissions are met for local display.
        // ViewModel handles backend sharing based on the preference.
        onLocationUpdate = { newLocation -> 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                viewModel.updateLocation(newLocation)
            } else {
                viewModel.updateLocation(newLocation)
            }
        },
        onUiMessage = { message -> uiMessage = message },
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
        if (uiMessage.isNotBlank()) {
            UiMessageBar(uiMessage = uiMessage)
        }
        MapArea(
            modifier = Modifier.weight(1f),
            currentMapLatLng = currentMapLatLng,
            defaultLocation = defaultLocation,
            batteryPercentage = batteryPercentage,
            userStatus = userStatus,
            shouldShowBattery = shouldShowBatteryOnMap,
            isLocationSharingPreferred = isLocationSharingPreferred, // For marker icon
            friendLocations = friendLocations,
            viewModel = viewModel
        )
        BottomNavBar(navController = navController, currentRoute = "map")
    }

    if (showPermissionRationaleDialog) {
        LocationPermissionRationaleDialog(
            onConfirm = {
                showPermissionRationaleDialog = false
                locationPermissionLauncher.launch(LOCATION_PERMISSIONS)
            },
            onDismiss = {
                showPermissionRationaleDialog = false
                uiMessage = MSG_PERM_RATIONALE_DISMISSED
                viewModel.updateLocation(null)
            }
        )
    }

    if (showLocationDisabledAlert) {
        LocationServicesDisabledDialog(
            context = context,
            onDismiss = { showLocationDisabledAlert = false }
        )
    }
}

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
        val activity = context as? Activity
        if (activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) == true ||
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) == true) {
            onShowRationale()
        } else {
            onRequestPermissions()
        }
    }
}

@Composable
private fun UiMessageBar(uiMessage: String) {
    val isErrorOrImportant = uiMessage == MSG_PERM_DENIED ||
            uiMessage == MSG_DEVICE_LOCATION_OFF ||
            uiMessage.contains("error", ignoreCase = true)
    // Removed MSG_SHARING_OFF_SETTINGS from error-like styling

    val backgroundColor = if (isErrorOrImportant) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = UI_MESSAGE_BACKGROUND_ALPHA)
    }
    val textColor = if (isErrorOrImportant) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Text(
        text = uiMessage,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        textAlign = TextAlign.Center, fontSize = 12.sp, color = textColor
    )
}

@Composable
private fun MapArea(
    modifier: Modifier = Modifier,
    currentMapLatLng: LatLng?,
    defaultLocation: LatLng,
    batteryPercentage: Int,
    userStatus: String,
    shouldShowBattery: Boolean,
    isLocationSharingPreferred: Boolean,
    friendLocations: List<com.familystalking.app.domain.model.FamilyMemberLocation>,
    viewModel: MapViewModel
) {
    Box(modifier = modifier.fillMaxWidth()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                currentMapLatLng ?: defaultLocation,
                if (currentMapLatLng != null) INITIAL_MAP_ZOOM else DEFAULT_MAP_NO_LOCATION_ZOOM
            )
        }
        MapCameraAnimator(cameraPositionState, currentMapLatLng, defaultLocation)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
        ) {
            // Display user's own marker
            currentMapLatLng?.let { validLatLng ->
                UserMarker(
                    position = validLatLng,
                    batteryPercentage = batteryPercentage,
                    userStatus = userStatus,
                    initials = "OP",
                    shouldShowBattery = shouldShowBattery,
                    isSharingLocation = isLocationSharingPreferred
                )
            }
            
            // Display friend markers
            friendLocations.forEach { friendLocation ->
                val friendLatLng = LatLng(
                    friendLocation.location.latitude,
                    friendLocation.location.longitude
                )
                FriendMarker(
                    position = friendLatLng,
                    friendName = friendLocation.name,
                    initials = viewModel.getFriendInitials(friendLocation.name),
                    friendLocation = friendLocation,
                    shouldShowBattery = shouldShowBattery
                )
            }
        }
    }
}

@Composable
private fun MapCameraAnimator(
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
            needsAnimation = cameraPositionState.position.target != defaultLocation ||
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
private fun UserMarker(
    position: LatLng,
    batteryPercentage: Int,
    userStatus: String,
    initials: String,
    shouldShowBattery: Boolean,
    isSharingLocation: Boolean
) {
    MarkerComposable(
        keys = arrayOf(batteryPercentage, userStatus, position, shouldShowBattery, isSharingLocation),
        state = MarkerState(position = position),
        anchor = Offset(MARKER_ANCHOR_X, MARKER_ANCHOR_Y)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MARKER_ELEMENT_SPACING_DP)
            ) {
                if (shouldShowBattery) {
                    BatteryIndicator(batteryPercentage = batteryPercentage)
                }
                Icon(
                    imageVector = if (isSharingLocation) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (isSharingLocation) "Location Sharing ON" else "Location Sharing OFF",
                    tint = if (isSharingLocation) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(SHARING_ICON_SIZE_DP)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            ProfileMarker(initials = initials)
            StatusIndicator(status = userStatus)
        }
    }
}

@Composable
private fun FriendMarker(
    position: LatLng,
    friendName: String,
    initials: String,
    friendLocation: com.familystalking.app.domain.model.FamilyMemberLocation,
    shouldShowBattery: Boolean
) {
    MarkerComposable(
        keys = arrayOf(friendName, position, friendLocation.location.timestamp),
        state = MarkerState(position = position),
        anchor = Offset(MARKER_ANCHOR_X, MARKER_ANCHOR_Y)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Show time banner if location is stale
            if (friendLocation.isLocationStale()) {
                Card(
                    modifier = Modifier.offset(y = (-8).dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = friendLocation.getFormattedTimeSinceUpdate(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = 10.sp
                    )
                }
            }
            
            // Sharing icon (always show as visible for friends)
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = "Friend Location",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SHARING_ICON_SIZE_DP)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            ProfileMarker(initials = initials)
            StatusIndicator(status = "Friend")
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
private fun ManageLocationUpdatesEffect(
    locationPermissionGranted: Boolean,
    // isLocationSharingPreferredSetting removed from params
    onLocationUpdate: (Location?) -> Unit,
    onUiMessage: (String) -> Unit,
    onShowLocationDisabledAlert: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, locationPermissionGranted) { // Only depends on permission now
        if (!locationPermissionGranted) {
            onLocationUpdate(null)
            onUiMessage(MSG_PERM_DENIED) // Keep this important message
            onDispose { Log.d(TAG, "MLUE: No permissions.") }
        } else {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                onShowLocationDisabledAlert(true)
                onLocationUpdate(null)
                onUiMessage(MSG_DEVICE_LOCATION_OFF) // Keep this important message
                onDispose { Log.d(TAG, "MLUE: Device location off.") }
            } else { // Permissions granted, device location on
                onShowLocationDisabledAlert(false)
                onUiMessage("") // Clear any previous message; tracking will commence

                val locationListener = createLocationListener(
                    context = context,
                    onLocationUpdate = onLocationUpdate,
                    onUiMessage = onUiMessage, // For provider status changes
                    onShowLocationDisabledAlert = onShowLocationDisabledAlert,
                    onReCheckAfterProviderEnabled = {
                        onUiMessage(MSG_PROVIDER_RE_ENABLED)
                    }
                )
                val params = LocationUpdateParams(
                    context, locationManager, locationListener,
                    isGpsEnabled, isNetworkEnabled, onLocationUpdate, onUiMessage
                )
                try {
                    requestLastKnownAndUpdates(params)
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: ${e.message}", e)
                    onLocationUpdate(null); onUiMessage(MSG_LOCATION_ERROR)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "IllegalArgumentException: ${e.message}", e)
                    onLocationUpdate(null); onUiMessage(MSG_LOCATION_CONFIG_ERROR)
                }
                onDispose {
                    Log.d(TAG, "Removing location updates.")
                    try { locationManager.removeUpdates(locationListener) }
                    catch (secEx: SecurityException) { Log.e(TAG, "SecEx rm: ${secEx.message}", secEx) }
                    catch (argEx: IllegalArgumentException) { Log.e(TAG, "ArgEx rm: ${argEx.message}", argEx) }
                }
            }
        }
    }
}

private fun createLocationListener(
    context: Context,
    onLocationUpdate: (Location?) -> Unit,
    onUiMessage: (String) -> Unit,
    onShowLocationDisabledAlert: (Boolean) -> Unit,
    onReCheckAfterProviderEnabled: () -> Unit
): LocationListener {
    return object : LocationListener {
        override fun onLocationChanged(location: Location) {
            onLocationUpdate(location)
        }
        override fun onProviderEnabled(provider: String) {
            // Message now indicates specific provider, not general tracking
            onUiMessage("$provider enabled.")
            onReCheckAfterProviderEnabled()
        }
        override fun onProviderDisabled(provider: String) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                onShowLocationDisabledAlert(true); onLocationUpdate(null); onUiMessage(MSG_PROVIDER_DISABLED_ALL_OFF)
            } else {
                onUiMessage(MSG_PROVIDER_DISABLED_SOME_OFF)
            }
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Log.d(TAG, "Provider $provider status: $status")
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestLastKnownAndUpdates(params: LocationUpdateParams) {
    var lastKnownLoc: Location? = null
    if (params.isGpsEnabled) lastKnownLoc = params.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    if (lastKnownLoc == null && params.isNetworkEnabled) lastKnownLoc = params.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    if (lastKnownLoc != null) {
        params.onLocationUpdate(lastKnownLoc)
        // No uiMessage set here for "tracking active"
    } else {
        // No uiMessage set here for "waiting for location"
        // Let MapScreen's initial state or other effect logic handle initial message if any
    }

    if (params.isGpsEnabled) params.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL_MS, LOCATION_UPDATE_MIN_DISTANCE_M, params.listener)
    if (params.isNetworkEnabled) params.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL_MS, LOCATION_UPDATE_MIN_DISTANCE_M, params.listener)
}

@Composable
fun LocationPermissionRationaleDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Permission Needed") },
        text = { Text("This app uses your location to show it on the map. Please grant the permission.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Grant") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LocationServicesDisabledDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Services Disabled") },
        text = { Text("Please enable GPS or Network location services for live location updates.") },
        confirmButton = { Button(onClick = { onDismiss(); context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Open Settings") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Dismiss") } }
    )
}

@Composable
fun BatteryIndicator(batteryPercentage: Int) {
    val batteryColor = when {
        batteryPercentage > BATTERY_LEVEL_GOOD -> Color(0xFF4CAF50)
        batteryPercentage > BATTERY_LEVEL_OKAY -> Color(0xFFFFA000)
        else -> Color(0xFFD32F2F)
    }
    Card(
        shape = BATTERY_INDICATOR_SHAPE,
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_DEFAULT_ELEVATION_DP),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = CARD_BACKGROUND_ALPHA))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BATTERY_INDICATOR_PADDING_HORIZONTAL_DP, vertical = BATTERY_INDICATOR_PADDING_VERTICAL_DP),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BATTERY_INDICATOR_INTERNAL_SPACING_DP)
        ) {
            val icon = when {
                batteryPercentage > BATTERY_LEVEL_FULL -> Icons.Filled.BatteryFull
                batteryPercentage > BATTERY_LEVEL_VERY_GOOD -> Icons.Filled.Battery5Bar
                batteryPercentage > BATTERY_LEVEL_GOOD -> Icons.Filled.Battery3Bar
                batteryPercentage > BATTERY_LEVEL_OKAY -> Icons.Filled.Battery2Bar
                batteryPercentage > BATTERY_LEVEL_LOW -> Icons.Filled.Battery1Bar
                else -> Icons.Filled.BatteryAlert
            }
            Icon(imageVector = icon, contentDescription = "Battery Level", tint = batteryColor, modifier = Modifier.size(BATTERY_ICON_SIZE_DP))
            Text(text = "$batteryPercentage%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = TEXT_PRIMARY_ALPHA))
        }
    }
}

@Composable
fun StatusIndicator(status: String) {
    Card(
        modifier = Modifier.padding(top = MARKER_STATUS_PADDING_TOP_DP),
        shape = STATUS_INDICATOR_SHAPE,
        elevation = CardDefaults.cardElevation(defaultElevation = CARD_DEFAULT_ELEVATION_DP),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = CARD_BACKGROUND_ALPHA)),
        border = BorderStroke(MARKER_STATUS_BORDER_WIDTH_DP, Color.Black.copy(alpha = UI_MESSAGE_BACKGROUND_ALPHA))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = MARKER_STATUS_PADDING_HORIZONTAL_DP, vertical = MARKER_STATUS_PADDING_VERTICAL_DP),
            contentAlignment = Alignment.Center
        ) {
            Text(text = status, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black.copy(alpha = TEXT_PRIMARY_ALPHA))
        }
    }
}

@Composable
fun ProfileMarker(initials: String) {
    Box(
        modifier = Modifier
            .size(MARKER_PROFILE_SIZE_DP)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(width = MARKER_BORDER_WIDTH_DP, color = MaterialTheme.colorScheme.onPrimaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}