package com.github.damontecres.wholphin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.preferences.AppThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BlueThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BoldBlueThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.BrownThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.GlassThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.GreenThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.OledThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.OrangeThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.PurpleThemeColors
import com.github.damontecres.wholphin.ui.theme.colors.RedThemeColors

val LocalTheme =
    compositionLocalOf<AppThemeColors> { AppThemeColors.GLASS }

fun getThemeColors(appThemeColors: AppThemeColors): ThemeColors =
    when (appThemeColors) {
        AppThemeColors.GLASS -> GlassThemeColors
        AppThemeColors.PURPLE -> PurpleThemeColors
        AppThemeColors.BLUE -> BlueThemeColors
        AppThemeColors.GREEN -> GreenThemeColors
        AppThemeColors.ORANGE -> OrangeThemeColors
        AppThemeColors.OLED_BLACK -> OledThemeColors
        AppThemeColors.BOLD_BLUE -> BoldBlueThemeColors
        AppThemeColors.RED -> RedThemeColors
        AppThemeColors.BROWN -> BrownThemeColors
        AppThemeColors.LAVENDER, AppThemeColors.AMBER, AppThemeColors.UNRECOGNIZED -> GlassThemeColors
    }

/**
 * Themes of greys only, which some components draw differently so they do not disappear
 */
val AppThemeColors.isMonochrome: Boolean
    get() = this == AppThemeColors.OLED_BLACK || this == AppThemeColors.GLASS

@Composable
fun WholphinTheme(
    darkTheme: Boolean = true,
    appThemeColors: AppThemeColors = AppThemeColors.GLASS,
    content: @Composable () -> Unit,
) {
    // Thorphin: every color is the Glass scheme with that color's accent and tint (ThemePalette.kt), so the same
    // elements change in every home screen style. Components that look at LocalTheme therefore always draw the Glass
    // way; upstream's own schemes (getThemeColors) are no longer used.
    val themeColors = remember(appThemeColors) { paletteThemeColors(appThemeColors.palette) }

    val colorScheme =
        when {
            darkTheme -> themeColors.darkScheme
            else -> themeColors.lightScheme
        }
    CompositionLocalProvider(LocalTheme provides AppThemeColors.GLASS) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = if (darkTheme) themeColors.darkSchemeMaterial else themeColors.lightSchemeMaterial,
            typography = androidx.compose.material3.Typography(),
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography,
                content = content,
            )
        }
    }
}
