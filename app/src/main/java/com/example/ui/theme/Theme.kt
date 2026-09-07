package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElegantDarkColorScheme = darkColorScheme(
  primary = ElegantPrimary,
  onPrimary = ElegantOnPrimary,
  primaryContainer = ElegantPrimaryContainer,
  onPrimaryContainer = ElegantOnPrimaryContainer,
  secondary = ElegantPrimary,
  onSecondary = ElegantOnPrimary,
  secondaryContainer = ElegantSurfaceVariant,
  onSecondaryContainer = ElegantTextPrimary,
  tertiary = ElegantOnPrimaryContainer,
  onTertiary = ElegantOnPrimary,
  background = ElegantBackground,
  onBackground = ElegantTextPrimary,
  surface = ElegantSurface,
  onSurface = ElegantTextPrimary,
  surfaceVariant = ElegantSurfaceVariant,
  onSurfaceVariant = ElegantTextSecondary,
  outline = ElegantBorder,
  outlineVariant = ElegantBorder.copy(alpha = 0.5f),
  error = ElegantError,
  onError = ElegantErrorContainer,
  errorContainer = ElegantErrorContainer,
  onErrorContainer = ElegantError,
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ElegantDarkColorScheme,
    typography = Typography,
    content = content
  )
}

