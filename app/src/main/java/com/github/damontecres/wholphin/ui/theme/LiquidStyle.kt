package com.github.damontecres.wholphin.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import com.github.damontecres.wholphin.ui.cards.LocalPosterCardShape
import com.github.damontecres.wholphin.ui.cards.PosterCardShape

/**
 * The "home screen style" the user chose, see [HomeScreenStyle]. Set once in ApplicationContent.
 */
val LocalHomeScreenStyle = compositionLocalOf { HomeScreenStyle.STYLE_CLASSIC }

/**
 * Set to the style by [LiquidStyleScope] and null elsewhere, so only the pages that opted in change their look
 */
val LocalLiquidStyle = compositionLocalOf<HomeScreenStyle?> { null }

/** Both Alternative UI designs share the side rail, the amber palette and the settings page */
val HomeScreenStyle.isAlternative: Boolean
    get() = this == HomeScreenStyle.STYLE_ALTERNATIVE || isMinimalist

/** The two Minimalist designs: the list of titles and the cards */
val HomeScreenStyle.isMinimalist: Boolean
    get() = this == HomeScreenStyle.STYLE_ALTERNATIVE_MINIMALIST || this == HomeScreenStyle.STYLE_ALTERNATIVE_MINIMALIST_CARDS

val HomeScreenStyle.isLiquid: Boolean
    get() = this != HomeScreenStyle.STYLE_CLASSIC

/**
 * Tokens of the Stitch "liquid glass" designs, see Theme Designs/DESIGN.md. With the Alternative UI they are the
 * amber "Thorphin TV" tokens of Theme Designs/Alternative UI instead, see [alternative].
 */
object LiquidColors {
    /** Set from the home screen style by ApplicationContent, a state so the colors recompose when it changes */
    var alternative by mutableStateOf(false)

    val Canvas get() = if (alternative) Color(0xFF0B0D11) else Color(0xFF08090C)
    val Base get() = if (alternative) Color(0xFF12151C) else Color(0xFF111218)
    val Elevated get() = if (alternative) Color(0xFF181C26) else Color(0xFF191B23)
    val Accent get() = if (alternative) Color(0xFFF3B744) else Color(0xFFC2C1FF)
    val TextPrimary get() = if (alternative) Color(0xFFFFFFFF) else Color(0xFFF5F5F7)
    val TextSecondary get() = if (alternative) Color(0xFF94A3B8) else Color(0xFFA7A9B4)
    val Rim get() = if (alternative) Color.White.copy(alpha = .06f) else Color.White.copy(alpha = .12f)

    /** Rail and panels of the Alternative UI, darker than [Base] */
    val Rail get() = if (alternative) Color(0xFF0C0E12) else Color(0xFF0B0C10)

    /** Text on top of an [Accent] filled surface */
    val OnAccent get() = if (alternative) Color(0xFF422C00) else Color(0xFF08090C)
}

/** Modern has round cards, Squared has almost square ones */
val HomeScreenStyle.cardShape: Shape
    get() =
        when (this) {
            HomeScreenStyle.STYLE_MODERN -> RoundedCornerShape(16.dp)
            HomeScreenStyle.STYLE_SQUARED -> RoundedCornerShape(4.dp)
            HomeScreenStyle.STYLE_ALTERNATIVE,
            HomeScreenStyle.STYLE_ALTERNATIVE_MINIMALIST,
            HomeScreenStyle.STYLE_ALTERNATIVE_MINIMALIST_CARDS,
            -> RoundedCornerShape(8.dp)
            else -> PosterCardShape
        }

/**
 * Gives [content] the colors and card corners of the chosen Modern or Squared style. With Classic it only composes
 * [content], so the page looks as it always did.
 */
@Composable
fun LiquidStyleScope(content: @Composable () -> Unit) {
    val style = LocalHomeScreenStyle.current
    if (!style.isLiquid) {
        content()
        return
    }
    val scheme =
        MaterialTheme.colorScheme.copy(
            primary = LiquidColors.Accent,
            background = LiquidColors.Canvas,
            surface = LiquidColors.Base,
            surfaceVariant = LiquidColors.Elevated,
            onSurface = LiquidColors.TextPrimary,
            onSurfaceVariant = LiquidColors.TextSecondary,
            border = LiquidColors.Rim,
        )
    CompositionLocalProvider(
        LocalLiquidStyle provides style,
        LocalPosterCardShape provides style.cardShape,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = MaterialTheme.shapes,
            typography =
                MaterialTheme.typography.copy(
                    // Row titles, as headline-lg of the designs
                    titleLarge = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
                ),
            content = content,
        )
    }
}
