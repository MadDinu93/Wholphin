package com.github.damontecres.wholphin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.GlassThemeColors

/**
 * What the "Theme colors" setting changes, the same in every home screen style: the [accent] (focus, the active tab
 * or rail entry, the Play button, progress bars, switches and sliders) with [onAccent] for text on it, and a light
 * tint of the backgrounds and panels in the accent color. [black] themes have pure black backgrounds instead.
 */
data class ThemePalette(
    val accent: Color,
    val onAccent: Color,
    val tinted: Boolean = true,
    val black: Boolean = false,
) {
    /** A panel color: [neutral] with a little of the accent mixed in, [strength] 1 is the normal tint */
    fun tint(
        neutral: Color,
        strength: Float = 1f,
    ): Color = if (tinted) lerp(neutral, accent, (TINT * strength).coerceIn(0f, 1f)) else neutral

    /** A page background: like [tint], but pure black for [black] themes */
    fun ground(neutral: Color): Color = if (black) Color.Black else tint(neutral)

    companion object {
        private const val TINT = .06f
    }
}

val AppThemeColors.palette: ThemePalette
    get() =
        when (this) {
            AppThemeColors.GLASS, AppThemeColors.UNRECOGNIZED -> ThemePalette(Color(0xFFE8E8EA), Color(0xFF141416), tinted = false)
            AppThemeColors.OLED_BLACK -> ThemePalette(Color(0xFFE8E8EA), Color(0xFF141416), tinted = false, black = true)
            AppThemeColors.LAVENDER -> ThemePalette(Color(0xFFC2C1FF), Color(0xFF11113A))
            AppThemeColors.PURPLE -> ThemePalette(Color(0xFFD2BCFF), Color(0xFF3D008F))
            AppThemeColors.BLUE -> ThemePalette(Color(0xFFAAC7FF), Color(0xFF09305F))
            AppThemeColors.BOLD_BLUE -> ThemePalette(Color(0xFF6EA8FF), Color(0xFF001A41))
            AppThemeColors.GREEN -> ThemePalette(Color(0xFF98D5A4), Color(0xFF00391A))
            AppThemeColors.AMBER -> ThemePalette(Color(0xFFF3B744), Color(0xFF422C00))
            AppThemeColors.ORANGE -> ThemePalette(Color(0xFFFF9F5A), Color(0xFF4A1F00))
            AppThemeColors.RED -> ThemePalette(Color(0xFFFFB4AA), Color(0xFF561E18))
            AppThemeColors.BROWN -> ThemePalette(Color(0xFFD9A97E), Color(0xFF3E2210))
        }

/** The order of the colors in the settings list, see R.array.app_theme_colors */
val ThemeColorChoices =
    listOf(
        AppThemeColors.GLASS,
        AppThemeColors.OLED_BLACK,
        AppThemeColors.LAVENDER,
        AppThemeColors.PURPLE,
        AppThemeColors.BLUE,
        AppThemeColors.BOLD_BLUE,
        AppThemeColors.GREEN,
        AppThemeColors.AMBER,
        AppThemeColors.ORANGE,
        AppThemeColors.RED,
        AppThemeColors.BROWN,
    )

/**
 * The Classic color scheme of [palette]: the grey Glass scheme with the accent roles taken from the palette and its
 * surfaces tinted, so the same elements change color as in the other home screen styles.
 */
fun paletteThemeColors(palette: ThemePalette): ThemeColors {
    val glass = GlassThemeColors
    val accent = palette.accent
    val onAccent = palette.onAccent
    // Containers are the grey of Glass with some accent, so they stay dark enough for light text
    val primaryContainer = lerp(Color(0xFF3C3C40), accent, if (palette.tinted) .35f else 0f)
    val secondaryContainer = palette.tint(Color(0xFF2C2C30), 2f)
    val tvBase = glass.darkScheme
    val m3Base = glass.darkSchemeMaterial
    val tvDark =
        tvBase.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainer,
            secondaryContainer = secondaryContainer,
            background = palette.ground(tvBase.background),
            surface = palette.ground(tvBase.surface),
            surfaceVariant = palette.tint(tvBase.surfaceVariant, 2f),
            inverseSurface = accent,
            inverseOnSurface = onAccent,
            inversePrimary = primaryContainer,
            border = accent.copy(alpha = .85f),
        )
    val m3Dark =
        m3Base.copy(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = primaryContainer,
            secondaryContainer = secondaryContainer,
            background = palette.ground(m3Base.background),
            surface = palette.ground(m3Base.surface),
            surfaceVariant = palette.tint(m3Base.surfaceVariant, 2f),
            inverseSurface = accent,
            inverseOnSurface = onAccent,
            inversePrimary = primaryContainer,
            surfaceDim = palette.ground(m3Base.surfaceDim),
            surfaceBright = palette.tint(m3Base.surfaceBright, 2f),
            surfaceContainerLowest = palette.ground(m3Base.surfaceContainerLowest),
            surfaceContainerLow = palette.tint(m3Base.surfaceContainerLow),
            surfaceContainer = palette.tint(m3Base.surfaceContainer, 1.5f),
            surfaceContainerHigh = palette.tint(m3Base.surfaceContainerHigh, 1.5f),
            surfaceContainerHighest = palette.tint(m3Base.surfaceContainerHighest, 2f),
        )
    return object : ThemeColors {
        override val lightSchemeMaterial = glass.lightSchemeMaterial
        override val lightScheme = glass.lightScheme
        override val darkSchemeMaterial = m3Dark
        override val darkScheme = tvDark
    }
}
