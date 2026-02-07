package com.example.theupsidedowncommunicator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val RetroColorScheme = darkColorScheme(
    primary = RetroGreen,
    secondary = RetroAmber,
    tertiary = RetroRed,
    background = RetroBlack,
    surface = RetroBlack,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = RetroGreen,
    onSurface = RetroGreen
)

@Composable
fun TheUpsideDownCommunicatorTheme(
    darkTheme: Boolean = true, // Always dark for retro feel
    dynamicColor: Boolean = false, // Disable dynamic color for consistency
    content: @Composable () -> Unit
) {
    val colorScheme = RetroColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}