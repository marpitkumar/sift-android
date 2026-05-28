package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    primaryContainer = SophisticatedPrimaryContainer,
    onPrimaryContainer = SophisticatedOnPrimaryContainer,
    secondary = SophisticatedSecondary,
    secondaryContainer = SophisticatedSecondaryContainer,
    onSecondaryContainer = SophisticatedOnSecondaryContainer,
    tertiary = SophisticatedTertiary,
    tertiaryContainer = SophisticatedTertiaryContainer,
    onTertiaryContainer = SophisticatedOnTertiaryContainer,
    background = SophisticatedBg,
    onBackground = SophisticatedOnBg,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnSurface,
    surfaceVariant = SophisticatedSurface,
    onSurfaceVariant = SophisticatedOnBg.copy(alpha = 0.7f),
    outline = SophisticatedOutline
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Disable dynamic color so we enforce our gorgeous Sophisticated Dark colors
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = SophisticatedDarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
