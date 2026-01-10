package com.example.nabu.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.nabu.utils.ThemeManager

@Composable
fun NabuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDynamicColorSupported = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // In a real app we might want to observe this flow, but for now we read on compose.
    // To make it reactive, we'd need a Flow in ThemeManager or SettingsManager.
    // For this implementation, changes will apply on recompose/restart.
    // Let's assume the parent refreshes or we rely on re-entry.
    // Ideally we should have `val currentTheme by SettingsManager.themeFlow.collectAsState()`
    val customTheme = remember(context) { ThemeManager.getTheme(context) } // This is static per composition, might need refresh mechanism.

    val colorScheme = when {
        isDynamicColorSupported && darkTheme -> dynamicDarkColorScheme(context)
        isDynamicColorSupported && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> {
            val theme = if (customTheme == ThemeManager.DEFAULT_LIGHT) ThemeManager.DEFAULT_DARK else customTheme
            darkColorScheme(
                primary = Color(theme.primary),
                onPrimary = Color(theme.onPrimary),
                primaryContainer = Color(theme.primaryContainer),
                onPrimaryContainer = Color(theme.onPrimaryContainer),
                secondary = Color(theme.secondary),
                onSecondary = Color(theme.onSecondary),
                secondaryContainer = Color(theme.secondaryContainer),
                onSecondaryContainer = Color(theme.onSecondaryContainer),
            )
        }
        else -> {
            lightColorScheme(
                primary = Color(customTheme.primary),
                onPrimary = Color(customTheme.onPrimary),
                primaryContainer = Color(customTheme.primaryContainer),
                onPrimaryContainer = Color(customTheme.onPrimaryContainer),
                secondary = Color(customTheme.secondary),
                onSecondary = Color(customTheme.onSecondary),
                secondaryContainer = Color(customTheme.secondaryContainer),
                onSecondaryContainer = Color(customTheme.onSecondaryContainer),
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}