package com.familystalking.app.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun NavBar(currentPage: String) {
    Row {
        NavItem("Map", currentPage, Icons.Default.Map)
        NavItem("Agenda", currentPage, Icons.Default.CalendarToday)
        NavItem("Family", currentPage, Icons.Default.Group)
        NavItem("Settings", currentPage, Icons.Default.Settings)
    }
}

@Composable
fun NavItem(label: String, currentPage: String, icon: ImageVector) {
    val itemColor = if (currentPage == label) Color.Green else Color.Black

    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = itemColor
    )
    Text(text = label, color = itemColor)
}

@Composable
fun AgendaItem(currentPage: String) {
    val agendaColor = if (currentPage == "Agenda") Color.Green else Color.Black

    Icon(
        imageVector = Icons.Default.CalendarToday,
        contentDescription = "Agenda",
        tint = agendaColor
    )
    Text(text = "Agenda", color = agendaColor)
}

@Composable
fun FamilyItem(currentPage: String) {
    val familyColor = if (currentPage == "Family") Color.Green else Color.Black

    Icon(
        imageVector = Icons.Default.Group,
        contentDescription = "Family",
        tint = familyColor
    )
    Text(text = "Family", color = familyColor)
}

@Composable
fun SettingsItem(currentPage: String) {
    val settingsColor = if (currentPage == "Settings") Color.Green else Color.Black

    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = settingsColor
    )
    Text(text = "Settings", color = settingsColor)
}
