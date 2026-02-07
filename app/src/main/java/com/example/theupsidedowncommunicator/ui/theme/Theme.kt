package com.example.theupsidedowncommunicator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RetroColorScheme = darkColorScheme(
    primary = RetroCyan,
    secondary = RetroTeal,
    tertiary = RetroRed,
    background = RetroBlack,
    surface = RetroBlack,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = RetroCyan,
    onSurface = RetroCyan
)

@Composable
fun TheUpsideDownCommunicatorTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = RetroColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}