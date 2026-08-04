package com.sentinelshield.antitheft.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme(
    val displayName: String,
    val primaryLight: Color,
    val primaryDark: Color,
    val secondaryLight: Color,
    val secondaryDark: Color,
    val tertiaryLight: Color,
    val tertiaryDark: Color,
    val backgroundLight: Color,
    val backgroundDark: Color,
    val isDynamic: Boolean = false,
) {
    Default(
        displayName = "Sentinel Shield",
        primaryLight = Color(0xFF2563EB),
        primaryDark = Color(0xFF60A5FA),
        secondaryLight = Color(0xFF1E40AF),
        secondaryDark = Color(0xFF93C5FD),
        tertiaryLight = Color(0xFF10B981),
        tertiaryDark = Color(0xFF34D399),
        backgroundLight = Color(0xFFF8FAFC),
        backgroundDark = Color(0xFF0B1220),
    ),
    Catppuccin(
        displayName = "Catppuccin",
        primaryLight = Color(0xFF4C6B9A),
        primaryDark = Color(0xFF9BA8CF),
        secondaryLight = Color(0xFFB76B8F),
        secondaryDark = Color(0xFFD4A5B8),
        tertiaryLight = Color(0xFFB8763E),
        tertiaryDark = Color(0xFF8AB8A8),
        backgroundLight = Color(0xFFEFF1F5),
        backgroundDark = Color(0xFF1E1E2E),
    ),
    Nord(
        displayName = "Nord",
        primaryLight = Color(0xFF5E81AC),
        primaryDark = Color(0xFF88C0D0),
        secondaryLight = Color(0xFF4C566A),
        secondaryDark = Color(0xFFD8DEE9),
        tertiaryLight = Color(0xFFB48EAD),
        tertiaryDark = Color(0xFFD8A9C4),
        backgroundLight = Color(0xFFECEFF4),
        backgroundDark = Color(0xFF2E3440),
    ),
    TokyoNight(
        displayName = "Tokyo Night",
        primaryLight = Color(0xFF3D5A80),
        primaryDark = Color(0xFF7D9BC1),
        secondaryLight = Color(0xFF6B5B95),
        secondaryDark = Color(0xFFA89DC9),
        tertiaryLight = Color(0xFF4A6B5C),
        tertiaryDark = Color(0xFF8AB4A3),
        backgroundLight = Color(0xFFF0F1F5),
        backgroundDark = Color(0xFF1A1B26),
    ),
    Sunset(
        displayName = "Sunset",
        primaryLight = Color(0xFFE65100),
        primaryDark = Color(0xFFFF9E80),
        secondaryLight = Color(0xFFEF6C00),
        secondaryDark = Color(0xFFFFCC80),
        tertiaryLight = Color(0xFFF4511E),
        tertiaryDark = Color(0xFFFF8A65),
        backgroundLight = Color(0xFFFFF5F0),
        backgroundDark = Color(0xFF1A120D),
    ),
    Dracula(
        displayName = "Dracula",
        primaryLight = Color(0xFF6272A4),
        primaryDark = Color(0xFFBD93F9),
        secondaryLight = Color(0xFF44475A),
        secondaryDark = Color(0xFFFF79C6),
        tertiaryLight = Color(0xFF50FA7B),
        tertiaryDark = Color(0xFF8BE9FD),
        backgroundLight = Color(0xFFF8F8F2),
        backgroundDark = Color(0xFF282A36),
    ),
    Lavender(
        displayName = "Lavender",
        primaryLight = Color(0xFF7C5AB8),
        primaryDark = Color(0xFFCFBCFF),
        secondaryLight = Color(0xFF635B70),
        secondaryDark = Color(0xFFCBC3DA),
        tertiaryLight = Color(0xFF7E525A),
        tertiaryDark = Color(0xFFF2B8C1),
        backgroundLight = Color(0xFFFCF8FF),
        backgroundDark = Color(0xFF16121A),
    ),
    Gruvbox(
        displayName = "Gruvbox",
        primaryLight = Color(0xFF9D5B3F),
        primaryDark = Color(0xFFD89B6A),
        secondaryLight = Color(0xFF7A7556),
        secondaryDark = Color(0xFFB0AE8A),
        tertiaryLight = Color(0xFF4A7B7C),
        tertiaryDark = Color(0xFF8AAFA8),
        backgroundLight = Color(0xFFFBF1C7),
        backgroundDark = Color(0xFF282828),
    ),
    GreenApple(
        displayName = "Green Apple",
        primaryLight = Color(0xFF2E7D32),
        primaryDark = Color(0xFF81C784),
        secondaryLight = Color(0xFF4A6349),
        secondaryDark = Color(0xFFB0CFB1),
        tertiaryLight = Color(0xFF3D7B5F),
        tertiaryDark = Color(0xFF8FD5B7),
        backgroundLight = Color(0xFFF6FFF6),
        backgroundDark = Color(0xFF0F1A0F),
    ),
    Midnight(
        displayName = "Midnight",
        primaryLight = Color(0xFF0D47A1),
        primaryDark = Color(0xFF90CAF9),
        secondaryLight = Color(0xFF455A64),
        secondaryDark = Color(0xFFB0BEC5),
        tertiaryLight = Color(0xFF1565C0),
        tertiaryDark = Color(0xFF64B5F6),
        backgroundLight = Color(0xFFF5F9FF),
        backgroundDark = Color(0xFF0D1117),
    ),
    Mocha(
        displayName = "Mocha",
        primaryLight = Color(0xFF795548),
        primaryDark = Color(0xFFBCAAA4),
        secondaryLight = Color(0xFF5D4037),
        secondaryDark = Color(0xFFA1887F),
        tertiaryLight = Color(0xFF6D4C41),
        tertiaryDark = Color(0xFFD7CCC8),
        backgroundLight = Color(0xFFFFF9F5),
        backgroundDark = Color(0xFF1A1512),
    ),
    Strawberry(
        displayName = "Strawberry",
        primaryLight = Color(0xFFD81B60),
        primaryDark = Color(0xFFF48FB1),
        secondaryLight = Color(0xFF6B4958),
        secondaryDark = Color(0xFFD6B0C1),
        tertiaryLight = Color(0xFFC2185B),
        tertiaryDark = Color(0xFFF8BBD9),
        backgroundLight = Color(0xFFFFF5F8),
        backgroundDark = Color(0xFF1A1015),
    ),
    Tidal(
        displayName = "Tidal",
        primaryLight = Color(0xFF00796B),
        primaryDark = Color(0xFF80CBC4),
        secondaryLight = Color(0xFF4A635E),
        secondaryDark = Color(0xFFB0CFC9),
        tertiaryLight = Color(0xFF00897B),
        tertiaryDark = Color(0xFF4DB6AC),
        backgroundLight = Color(0xFFF2FFFD),
        backgroundDark = Color(0xFF0F1A18),
    ),
    Forest(
        displayName = "Forest",
        primaryLight = Color(0xFF1B5E20),
        primaryDark = Color(0xFF66BB6A),
        secondaryLight = Color(0xFF33691E),
        secondaryDark = Color(0xFF9CCC65),
        tertiaryLight = Color(0xFF2E7D32),
        tertiaryDark = Color(0xFFA5D6A7),
        backgroundLight = Color(0xFFF1F8E9),
        backgroundDark = Color(0xFF0D1A0D),
    ),
    RoseGold(
        displayName = "Rose Gold",
        primaryLight = Color(0xFFB76E79),
        primaryDark = Color(0xFFE8A9B0),
        secondaryLight = Color(0xFFAD8075),
        secondaryDark = Color(0xFFDDBFB8),
        tertiaryLight = Color(0xFFD4A5A5),
        tertiaryDark = Color(0xFFF5D5D5),
        backgroundLight = Color(0xFFFFF5F5),
        backgroundDark = Color(0xFF1A1315),
    ),
    Monochrome(
        displayName = "Monochrome",
        primaryLight = Color(0xFF212121),
        primaryDark = Color(0xFFE0E0E0),
        secondaryLight = Color(0xFF424242),
        secondaryDark = Color(0xFFBDBDBD),
        tertiaryLight = Color(0xFF616161),
        tertiaryDark = Color(0xFF9E9E9E),
        backgroundLight = Color(0xFFFFFFFF),
        backgroundDark = Color(0xFF0A0A0A),
    );

    fun getLightColorScheme(): ColorScheme {
        val surfaceTint = primaryLight.copy(alpha = 0.08f).compositeOver(backgroundLight)
        return lightColorScheme(
            primary = primaryLight,
            onPrimary = Color.White,
            primaryContainer = primaryLight.copy(alpha = 0.16f).compositeOver(Color.White),
            onPrimaryContainer = primaryLight.darken(0.35f),
            secondary = secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = secondaryLight.copy(alpha = 0.16f).compositeOver(Color.White),
            onSecondaryContainer = secondaryLight.darken(0.35f),
            tertiary = tertiaryLight,
            onTertiary = Color.White,
            background = backgroundLight,
            onBackground = Color(0xFF0F172A),
            surface = backgroundLight,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = primaryLight.copy(alpha = 0.10f).compositeOver(Color(0xFFF1F5F9)),
            onSurfaceVariant = Color(0xFF475569),
            outline = secondaryLight.copy(alpha = 0.5f),
            outlineVariant = primaryLight.copy(alpha = 0.15f),
            surfaceContainerLow = surfaceTint,
            surfaceContainer = primaryLight.copy(alpha = 0.06f).compositeOver(backgroundLight),
            surfaceContainerHigh = primaryLight.copy(alpha = 0.10f).compositeOver(backgroundLight),
            surfaceContainerHighest = primaryLight.copy(alpha = 0.14f).compositeOver(backgroundLight),
        )
    }

    fun getDarkColorScheme(): ColorScheme {
        val surfaceTint = primaryDark.copy(alpha = 0.08f).compositeOver(backgroundDark)
        return darkColorScheme(
            primary = primaryDark,
            onPrimary = primaryLight.darken(0.5f),
            primaryContainer = primaryLight.darken(0.2f),
            onPrimaryContainer = primaryDark.lighten(0.15f),
            secondary = secondaryDark,
            onSecondary = secondaryLight.darken(0.5f),
            secondaryContainer = secondaryLight.darken(0.2f),
            onSecondaryContainer = secondaryDark.lighten(0.15f),
            tertiary = tertiaryDark,
            onTertiary = tertiaryLight.darken(0.5f),
            background = backgroundDark,
            onBackground = Color(0xFFF1F5F9),
            surface = backgroundDark,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = primaryDark.copy(alpha = 0.15f).compositeOver(Color(0xFF1E293B)),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = secondaryDark.copy(alpha = 0.4f),
            outlineVariant = primaryDark.copy(alpha = 0.18f),
            surfaceContainerLowest = backgroundDark.darken(0.2f),
            surfaceContainerLow = surfaceTint,
            surfaceContainer = primaryDark.copy(alpha = 0.06f).compositeOver(backgroundDark),
            surfaceContainerHigh = primaryDark.copy(alpha = 0.10f).compositeOver(backgroundDark),
            surfaceContainerHighest = primaryDark.copy(alpha = 0.14f).compositeOver(backgroundDark),
        )
    }

    fun getAmoledColorScheme(): ColorScheme = getDarkColorScheme().copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF121212)),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainerLowest = Color.Black,
        surfaceContainerHigh = primaryDark.copy(alpha = 0.05f).compositeOver(Color(0xFF151515)),
        surfaceContainerHighest = primaryDark.copy(alpha = 0.08f).compositeOver(Color(0xFF1F1F1F)),
    )
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1 - factor)).coerceIn(0f, 1f),
        green = (green * (1 - factor)).coerceIn(0f, 1f),
        blue = (blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1 - red) * factor).coerceIn(0f, 1f),
        green = (green + (1 - green) * factor).coerceIn(0f, 1f),
        blue = (blue + (1 - blue) * factor).coerceIn(0f, 1f),
        alpha = alpha
    )
}

private fun Color.compositeOver(background: Color): Color {
    val bgAlpha = background.alpha
    val fgAlpha = alpha
    val a = fgAlpha + bgAlpha * (1f - fgAlpha)
    return if (a == 0f) {
        Color.Transparent
    } else {
        Color(
            red = (red * fgAlpha + background.red * bgAlpha * (1f - fgAlpha)) / a,
            green = (green * fgAlpha + background.green * bgAlpha * (1f - fgAlpha)) / a,
            blue = (blue * fgAlpha + background.blue * bgAlpha * (1f - fgAlpha)) / a,
            alpha = a
        )
    }
}
