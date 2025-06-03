package com.familystalking.app.presentation.settings

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.BottomNavBar
import com.familystalking.app.presentation.navigation.Screen // Ensure this import is correct

// Constants
private val BACKGROUND_COLOR = Color(0xFFF5F5F5)
private val CARD_CORNER_RADIUS = 8.dp
private val SETTINGS_CARD_VERTICAL_PADDING = 8.dp
private val CARD_PADDING = 16.dp
private val AVATAR_SIZE = 40.dp
private val AVATAR_COLOR = Color.LightGray
private val AVATAR_TEXT_COLOR = Color.White
private val SECTION_TITLE_FONT_SIZE = 14.sp
private const val SWITCH_SCALE = 0.75f
private val DIVIDER_COLOR = Color(0xFFEEEEEE)
private val SIGN_OUT_COLOR = Color.Red
private val SIGN_OUT_INDICATOR_COLOR = Color.White
private val SWITCH_CHECKED_TRACK_COLOR: Color @Composable get() = MaterialTheme.colorScheme.primary
private val SWITCH_UNCHECKED_TRACK_COLOR = Color.Gray.copy(alpha = 0.5f)
private val SWITCH_DISABLED_CHECKED_TRACK_COLOR: Color @Composable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
private val SWITCH_DISABLED_UNCHECKED_TRACK_COLOR = Color.Gray.copy(alpha = 0.3f)


@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val userLocationSharingPreference by viewModel.locationSharingPreference.collectAsState()
    val isDeviceLocationEnabled by viewModel.isDeviceLocationEnabled.collectAsState()

    val pushNotifications by viewModel.pushNotifications.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()

    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        viewModel.updateDeviceLocationStatus(gpsEnabled || networkEnabled)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(CARD_PADDING)
        ) {
            UserProfileCard(userName = userName)

            SettingsCard(
                isDeviceLocationEnabled = isDeviceLocationEnabled,
                userLocationSharingPreference = userLocationSharingPreference,
                onLocationSharingPreferenceChange = viewModel::toggleLocationSharingPreference,
                pushNotifications = pushNotifications,
                onPushNotificationsChange = viewModel::togglePushNotifications,
                showBatteryPercentage = showBatteryPercentage,
                onShowBatteryPercentageChange = viewModel::toggleShowBatteryPercentage,
                onEnableDeviceLocationClicked = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            )

            SignOutCard(
                isLoading = isLoading,
                onSignOut = {
                    if (!isLoading) {
                        viewModel.signOut()
                    }
                }
            )
        }
        BottomNavBar(
            currentRoute = Screen.Settings.route,
            navController = navController
        )
    }
}

@Composable
private fun UserProfileCard(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CARD_PADDING),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .background(AVATAR_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.takeIf { it.isNotEmpty() }?.take(2)?.uppercase() ?: "U",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AVATAR_TEXT_COLOR
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = CARD_PADDING)
                    .weight(1f)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Edit your name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    isDeviceLocationEnabled: Boolean,
    userLocationSharingPreference: Boolean,
    onLocationSharingPreferenceChange: () -> Unit,
    pushNotifications: Boolean,
    onPushNotificationsChange: () -> Unit,
    showBatteryPercentage: Boolean,
    onShowBatteryPercentageChange: () -> Unit,
    onEnableDeviceLocationClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CARD_PADDING),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SETTINGS_CARD_VERTICAL_PADDING)
        ) {
            SectionTitle("Privacy")
            LocationSharingSettingItem(
                isDeviceLocationEnabled = isDeviceLocationEnabled,
                userPreferenceEnabled = userLocationSharingPreference,
                onUserPreferenceChange = onLocationSharingPreferenceChange,
                onEnableDeviceLocationClicked = onEnableDeviceLocationClicked
            )

            SettingsDivider()
            SectionTitle("Notifications")
            SettingsToggleItem(
                title = "Push notifications",
                checked = pushNotifications,
                onCheckedChange = onPushNotificationsChange
            )

            SettingsDivider()
            SectionTitle("Map settings")
            SettingsToggleItem(
                title = "Show battery percentage",
                checked = showBatteryPercentage,
                onCheckedChange = onShowBatteryPercentageChange
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = CARD_PADDING, vertical = 4.dp)
    )
}

@Composable
private fun LocationSharingSettingItem(
    isDeviceLocationEnabled: Boolean,
    userPreferenceEnabled: Boolean,
    onUserPreferenceChange: () -> Unit,
    onEnableDeviceLocationClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isDeviceLocationEnabled, onClick = {
                    if (isDeviceLocationEnabled) onUserPreferenceChange()
                })
                .padding(horizontal = CARD_PADDING, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location sharing",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDeviceLocationEnabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
                fontSize = SECTION_TITLE_FONT_SIZE,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = userPreferenceEnabled && isDeviceLocationEnabled,
                onCheckedChange = {
                    if (isDeviceLocationEnabled) {
                        onUserPreferenceChange()
                    }
                },
                enabled = isDeviceLocationEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                    checkedTrackColor = SWITCH_CHECKED_TRACK_COLOR,
                    uncheckedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                    uncheckedTrackColor = SWITCH_UNCHECKED_TRACK_COLOR,
                    disabledCheckedTrackColor = SWITCH_DISABLED_CHECKED_TRACK_COLOR,
                    disabledUncheckedTrackColor = SWITCH_DISABLED_UNCHECKED_TRACK_COLOR
                ),
                modifier = Modifier.scale(SWITCH_SCALE)
            )
        }
        if (!isDeviceLocationEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = CARD_PADDING, end = CARD_PADDING, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device location is off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEnableDeviceLocationClicked) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onCheckedChange)
            .padding(horizontal = CARD_PADDING, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray,
            fontSize = SECTION_TITLE_FONT_SIZE,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                checkedTrackColor = SWITCH_CHECKED_TRACK_COLOR,
                uncheckedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                uncheckedTrackColor = SWITCH_UNCHECKED_TRACK_COLOR,
                disabledCheckedTrackColor = SWITCH_DISABLED_CHECKED_TRACK_COLOR,
                disabledUncheckedTrackColor = SWITCH_DISABLED_UNCHECKED_TRACK_COLOR
            ),
            modifier = Modifier.scale(SWITCH_SCALE)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        modifier = Modifier.padding(horizontal = CARD_PADDING, vertical = SETTINGS_CARD_VERTICAL_PADDING),
        color = DIVIDER_COLOR,
        thickness = 1.dp
    )
}

@Composable
private fun SignOutCard(isLoading: Boolean, onSignOut: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SETTINGS_CARD_VERTICAL_PADDING),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) { onSignOut() }
                .padding(CARD_PADDING),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SIGN_OUT_COLOR,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign out",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = SIGN_OUT_COLOR
                )
            }
        }
    }
}