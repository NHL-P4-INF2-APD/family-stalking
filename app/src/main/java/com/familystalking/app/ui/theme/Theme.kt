package com.familystalking.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    background = Color.White,
    onBackground = OnBackgroundColor,
    error = ErrorColor,
    surface = Color.White,
    onSurface = OnBackgroundColor,
    surfaceVariant = TextFieldBackgroundColor,
    onSurfaceVariant = OnBackgroundColor.copy(alpha = 0.6f)
)

@Composable
fun FamilyStalkingTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.White.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
