package com.familystalking.app.presentation.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.Screen
import com.familystalking.app.presentation.navigation.bottomNavBar

private val BACKGROUND_COLOR = Color(0xFFF5F5F5)
private val CARD_CORNER_RADIUS = 8.dp
private val CARD_PADDING = 16.dp
private val AVATAR_SIZE = 40.dp
private val AVATAR_COLOR = Color.LightGray
private val AVATAR_TEXT_COLOR = Color.White
private val SECTION_TITLE_FONT_SIZE = 14.sp
private const val SWITCH_SCALE = 0.65f
private val DIVIDER_COLOR = Color(0xFFEEEEEE)
private val SIGN_OUT_COLOR = Color.Red
private val SIGN_OUT_INDICATOR_COLOR = Color.White

data class SettingsToggles(
    val locationSharing: Boolean,
    val onLocationSharingChange: () -> Unit,
    val pushNotifications: Boolean,
    val onPushNotificationsChange: () -> Unit,
    val showBatteryPercentage: Boolean,
    val onShowBatteryPercentageChange: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val locationSharing by viewModel.locationSharing.collectAsState()
    val pushNotifications by viewModel.pushNotifications.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BACKGROUND_COLOR)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(CARD_PADDING)
        ) {
            userProfileCard(userName)
            settingsCard(
                toggles = SettingsToggles(
                    locationSharing = locationSharing,
                    onLocationSharingChange = viewModel::toggleLocationSharing,
                    pushNotifications = pushNotifications,
                    onPushNotificationsChange = viewModel::togglePushNotifications,
                    showBatteryPercentage = showBatteryPercentage,
                    onShowBatteryPercentageChange = viewModel::toggleShowBatteryPercentage
                )
            )
            signOutCard(isLoading = isLoading, onSignOut = viewModel::signOut)
        }
        bottomNavBar(
            currentRoute = Screen.Settings.route,
            navController = navController
        )
    }
}

@Composable
private fun userProfileCard(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CARD_PADDING),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
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
                    text = userName.take(2).uppercase(),
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
private fun settingsCard(
    toggles: SettingsToggles
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = CARD_PADDING),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Privacy section
            Text(
                text = "Privacy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = CARD_PADDING, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CARD_PADDING, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Location sharing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = SECTION_TITLE_FONT_SIZE
                )
                Switch(
                    checked = toggles.locationSharing,
                    onCheckedChange = { toggles.onLocationSharingChange() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        uncheckedTrackColor = Color.Gray.copy(alpha = SWITCH_SCALE)
                    ),
                    modifier = Modifier.scale(SWITCH_SCALE)
                )
            }
            HorizontalDivider()
            // Notifications section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = CARD_PADDING, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CARD_PADDING, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Push notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = SECTION_TITLE_FONT_SIZE
                )
                Switch(
                    checked = toggles.pushNotifications,
                    onCheckedChange = { toggles.onPushNotificationsChange() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        uncheckedTrackColor = Color.Gray.copy(alpha = SWITCH_SCALE)
                    ),
                    modifier = Modifier.scale(SWITCH_SCALE)
                )
            }
            HorizontalDivider()
            // Map settings section
            Text(
                text = "Map settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = CARD_PADDING, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CARD_PADDING, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Show battery percentage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = SECTION_TITLE_FONT_SIZE
                )
                Switch(
                    checked = toggles.showBatteryPercentage,
                    onCheckedChange = { toggles.onShowBatteryPercentageChange() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = SIGN_OUT_INDICATOR_COLOR,
                        uncheckedTrackColor = Color.Gray.copy(alpha = SWITCH_SCALE)
                    ),
                    modifier = Modifier.scale(SWITCH_SCALE)
                )
            }
        }
    }
}

@Composable
private fun signOutCard(isLoading: Boolean, onSignOut: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSignOut() }
                .padding(CARD_PADDING),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = SIGN_OUT_COLOR
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
