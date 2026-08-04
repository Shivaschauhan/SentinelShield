package com.sentinelshield.antitheft.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

enum class DarkModeOption {
    System,
    Light,
    Dark
}

@Composable
fun SentinelShieldTheme(
    appTheme: AppTheme = AppTheme.Default,
    darkMode: DarkModeOption = DarkModeOption.System,
    amoledMode: Boolean = false,
    useSystemFont: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        DarkModeOption.System -> systemDark
        DarkModeOption.Light -> false
        DarkModeOption.Dark -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDark && amoledMode) {
                dynamicScheme.copy(
                    background = androidx.compose.ui.graphics.Color.Black,
                    surface = androidx.compose.ui.graphics.Color.Black,
                    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF0A0A0A)
                )
            } else dynamicScheme
        }
        isDark && amoledMode -> appTheme.getAmoledColorScheme()
        isDark -> appTheme.getDarkColorScheme()
        else -> appTheme.getLightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
