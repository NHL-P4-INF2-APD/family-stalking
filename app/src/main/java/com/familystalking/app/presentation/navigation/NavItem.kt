package com.familystalking.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Map : NavItem(
        route = "map",
        title = "Map",
        icon = Icons.Default.Map
    )

    object Agenda : NavItem(
        route = "agenda",
        title = "Agenda",
        icon = Icons.Default.CalendarToday
    )

    object Family : NavItem(
        route = "family",
        title = "Family",
        icon = Icons.Default.Group
    )

    object Settings : NavItem(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )

    companion object {
        val items = listOf(Map, Agenda, Family, Settings)
    }
}
