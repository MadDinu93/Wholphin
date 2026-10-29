package com.github.damontecres.wholphin.ui.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * Apple TV style glass look for a focused card: a thin rim that catches the light along the top, a soft gloss from
 * the top left and a sheen that sweeps across once when the card gets focus.
 *
 * Goes on the content inside the card, which is clipped to the same [shape]. It only draws, so nothing is composed or
 * laid out again while it animates.
 */
@Composable
fun Modifier.glassHighlight(
    interactionSource: InteractionSource,
    shape: Shape = LocalPosterCardShape.current,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val focusAlpha = animateFloatAsState(if (focused) 1f else 0f, tween(if (focused) 250 else 150), label = "glass")
    val sheen = remember { Animatable(1f) }
    LaunchedEffect(focused) {
        if (focused) {
            sheen.snapTo(0f)
            sheen.animateTo(1f, tween(durationMillis = 1100, delayMillis = 150, easing = FastOutSlowInEasing))
        }
    }
    return drawWithCache {
        val rimWidth = 1.5.dp.toPx()
        val rim =
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = .95f),
                .3f to Color.White.copy(alpha = .4f),
                .7f to Color.White.copy(alpha = .15f),
                1f to Color.White.copy(alpha = .4f),
            )
        val gloss =
            Brush.linearGradient(
                0f to Color.White.copy(alpha = .2f),
                .45f to Color.Transparent,
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            )
        // Positions along the top left to bottom right diagonal are measured as x + y
        val diagonal = size.width + size.height
        val band = diagonal * .3f
        onDrawWithContent {
            drawContent()
            val alpha = focusAlpha.value
            if (alpha <= 0f) return@onDrawWithContent

            drawRect(gloss, alpha = alpha)

            val progress = sheen.value
            if (progress < 1f) {
                val center = -band + progress * (diagonal + 2 * band)
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Transparent,
                        .5f to Color.White.copy(alpha = .22f),
                        1f to Color.Transparent,
                        start = Offset((center - band) / 2, (center - band) / 2),
                        end = Offset((center + band) / 2, (center + band) / 2),
                    ),
                    alpha = alpha,
                )
            }

            // A soft glow just inside the edge gives the glass its thickness, the rim is where it catches the light
            innerRim(shape, SolidColor(Color.White.copy(alpha = .05f)), 10.dp.toPx(), alpha)
            innerRim(shape, SolidColor(Color.White.copy(alpha = .1f)), 4.dp.toPx(), alpha)
            innerRim(shape, rim, rimWidth, alpha)
        }
    }
}

/**
 * Strokes the [shape] just inside the edge
 */
private fun DrawScope.innerRim(
    shape: Shape,
    brush: Brush,
    width: Float,
    alpha: Float,
) {
    val outline = shape.createOutline(Size(size.width - width, size.height - width), layoutDirection, this)
    translate(width / 2, width / 2) {
        drawOutline(outline, brush, alpha = alpha, style = Stroke(width))
    }
}
