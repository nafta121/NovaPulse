package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = PolishIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = PolishBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = PolishPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = PolishBgDark,
    onBackground = PolishTextPrimaryDark,
    surface = PolishSurfaceDark,
    onSurface = PolishTextPrimaryDark,
    surfaceVariant = PolishCardDark,
    onSurfaceVariant = PolishTextSecondaryDark,
    outline = PolishBorderDark,
    error = PolishRose,
    onError = Color.White,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PolishIndigo,
    onPrimary = Color.White,
    primaryContainer = PolishIndigoLight,
    onPrimaryContainer = PolishIndigoDark,
    secondary = PolishBlue,
    onSecondary = Color.White,
    secondaryContainer = PolishBlueLight,
    onSecondaryContainer = Color(0xFF1D4ED8),
    tertiary = PolishPurple,
    onTertiary = Color.White,
    tertiaryContainer = PolishPurpleLight,
    onTertiaryContainer = Color(0xFF6D28D9),
    background = PolishBgLight,
    onBackground = PolishTextPrimaryLight,
    surface = PolishSurfaceLight,
    onSurface = PolishTextPrimaryLight,
    surfaceVariant = PolishCardSubtleLight,
    onSurfaceVariant = PolishTextSecondaryLight,
    outline = PolishBorderLight,
    error = PolishRose,
    onError = Color.White,
  )

@Composable
fun NovaPulseTheme(
  darkTheme: Boolean = false, // Pure clean light glassmorphism theme for Samsung Now Briefing
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}


