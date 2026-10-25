package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import com.github.damontecres.wholphin.data.model.BaseItem
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale

/** Corner radius of image cards, rounder than the default for an Apple TV look */
val PosterCardCornerRadius = 12.dp

/** Rounded corners for image cards, rounder than the default for an Apple TV look */
val PosterCardShape = RoundedCornerShape(PosterCardCornerRadius)

/** No outline on focus, [glassHighlight] draws the edge instead */
@Composable
fun posterCardBorder(): CardBorder = CardDefaults.border(focusedBorder = Border.None)

/** Focused cards lift towards the viewer */
@Composable
fun posterCardScale() = CardDefaults.scale(focusedScale = 1.1f)

/**
 * "Genre · ★ 8.3" for a movie or series, shown at the top of its poster
 */
internal fun BaseItem.posterInfoText(): String? {
    if (type != BaseItemKind.MOVIE && type != BaseItemKind.SERIES) return null
    val genre = data.genres?.firstOrNull()
    val rating =
        data.communityRating
            ?.takeIf { it > 0 }
            ?.let { "★ " + String.format(Locale.getDefault(), "%.1f", it) }
    return listOfNotNull(genre, rating).joinToString(" · ").ifBlank { null }
}
