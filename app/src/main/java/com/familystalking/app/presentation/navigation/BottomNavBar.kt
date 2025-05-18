package com.familystalking.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.familystalking.app.presentation.navigation.NavItem

@Composable
fun BottomNavBar(
    currentRoute: String,
    navController: NavController
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavItem.items.forEach { navItem ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = navItem.icon,
                        contentDescription = navItem.title
                    )
                },
                label = {
                    Text(
                        text = navItem.title,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                selected = currentRoute == navItem.route,
                onClick = {
                    if (currentRoute != navItem.route) {
                        navController.navigate(navItem.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
