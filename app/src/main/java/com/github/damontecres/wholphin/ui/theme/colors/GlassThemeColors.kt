package com.github.damontecres.wholphin.ui.theme.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.darkColorScheme
import com.github.damontecres.wholphin.ui.theme.ThemeColors

/**
 * Thorphin's Apple TV like look: greys, white and black only, so the artwork brings the colour
 */
val GlassThemeColors =
    object : ThemeColors {
        val primaryDark = Color(0xFFE8E8EA)
        val onPrimaryDark = Color(0xFF141416)
        val primaryContainerDark = Color(0xFF3C3C40)
        val onPrimaryContainerDark = Color(0xFFF2F2F4)
        val secondaryDark = Color(0xFFB4B4B8)
        val onSecondaryDark = Color(0xFF1A1A1C)
        val secondaryContainerDark = Color(0xFF2C2C30)
        val onSecondaryContainerDark = Color(0xFFEDEDEF)
        val tertiaryDark = Color(0xFFF5F5F7)
        val onTertiaryDark = Color(0xFF1A1A1C)
        val tertiaryContainerDark = Color(0xFF505054)
        val onTertiaryContainerDark = Color(0xFFFFFFFF)
        val errorDark = Color(0xFFFFB4AB)
        val onErrorDark = Color(0xFF690005)
        val errorContainerDark = Color(0xFF93000A)
        val onErrorContainerDark = Color(0xFFFFFFFF)
        val backgroundDark = Color(0xFF0B0B0D)
        val onBackgroundDark = Color(0xFFF2F2F4)
        val surfaceDark = Color(0xFF0F0F11)
        val onSurfaceDark = Color(0xFFF2F2F4)
        val surfaceVariantDark = Color(0xFF2A2A2E)
        val onSurfaceVariantDark = Color(0xFFC7C7CC)
        val outlineDark = Color(0xFF8E8E93)
        val outlineVariantDark = Color(0xFF3A3A3C)
        val scrimDark = Color(0xFF000000)
        val inverseSurfaceDark = Color(0xFFF2F2F4)
        val inverseOnSurfaceDark = Color(0xFF111113)
        val inversePrimaryDark = Color(0xFF3C3C40)

        /** Focus outlines, a bright frosted white */
        val borderDark = Color(0xD9FFFFFF)

        // Only the dark theme is used, the light one is borrowed
        override val lightSchemeMaterial: ColorScheme = OledThemeColors.lightSchemeMaterial
        override val lightScheme = OledThemeColors.lightScheme

        override val darkSchemeMaterial =
            androidx.compose.material3.darkColorScheme(
                primary = primaryDark,
                onPrimary = onPrimaryDark,
                primaryContainer = primaryContainerDark,
                onPrimaryContainer = onPrimaryContainerDark,
                secondary = secondaryDark,
                onSecondary = onSecondaryDark,
                secondaryContainer = secondaryContainerDark,
                onSecondaryContainer = onSecondaryContainerDark,
                tertiary = tertiaryDark,
                onTertiary = onTertiaryDark,
                tertiaryContainer = tertiaryContainerDark,
                onTertiaryContainer = onTertiaryContainerDark,
                error = errorDark,
                onError = onErrorDark,
                errorContainer = errorContainerDark,
                onErrorContainer = onErrorContainerDark,
                background = backgroundDark,
                onBackground = onBackgroundDark,
                surface = surfaceDark,
                onSurface = onSurfaceDark,
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = onSurfaceVariantDark,
                outline = outlineDark,
                outlineVariant = outlineVariantDark,
                scrim = scrimDark,
                inverseSurface = inverseSurfaceDark,
                inverseOnSurface = inverseOnSurfaceDark,
                inversePrimary = inversePrimaryDark,
                surfaceDim = Color(0xFF0B0B0D),
                surfaceBright = Color(0xFF3A3A3E),
                surfaceContainerLowest = Color(0xFF08080A),
                surfaceContainerLow = Color(0xFF161618),
                surfaceContainer = Color(0xFF1C1C1E),
                surfaceContainerHigh = Color(0xFF26262A),
                surfaceContainerHighest = Color(0xFF303034),
            )

        override val darkScheme =
            darkColorScheme(
                primary = primaryDark,
                onPrimary = onPrimaryDark,
                primaryContainer = primaryContainerDark,
                onPrimaryContainer = onPrimaryContainerDark,
                secondary = secondaryDark,
                onSecondary = onSecondaryDark,
                secondaryContainer = secondaryContainerDark,
                onSecondaryContainer = onSecondaryContainerDark,
                tertiary = tertiaryDark,
                onTertiary = onTertiaryDark,
                tertiaryContainer = tertiaryContainerDark,
                onTertiaryContainer = onTertiaryContainerDark,
                error = errorDark,
                onError = onErrorDark,
                errorContainer = errorContainerDark,
                onErrorContainer = onErrorContainerDark,
                background = backgroundDark,
                onBackground = onBackgroundDark,
                surface = surfaceDark,
                onSurface = onSurfaceDark,
                surfaceVariant = surfaceVariantDark,
                onSurfaceVariant = onSurfaceVariantDark,
                scrim = scrimDark,
                inverseSurface = inverseSurfaceDark,
                inverseOnSurface = inverseOnSurfaceDark,
                inversePrimary = inversePrimaryDark,
                border = borderDark,
            )
    }
