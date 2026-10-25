package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.data.model.BaseItem
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Locale

/** Rounded corners for image cards, rounder than the default for an Apple TV look */
val PosterCardShape = RoundedCornerShape(12.dp)

/** The default card focus border, but following [PosterCardShape] */
@Composable
fun posterCardBorder(): CardBorder =
    CardDefaults.border(
        focusedBorder =
            Border(
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.border),
                shape = PosterCardShape,
            ),
    )

/**
 * Shows "Genre · ★ 8.3" along the bottom of a movie or series poster
 */
@Composable
fun BoxScope.PosterInfoOverlay(
    item: BaseItem?,
    modifier: Modifier = Modifier,
) {
    val text = remember(item) { item?.posterInfoText() } ?: return
    Box(
        modifier =
            modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .75f))),
                ).padding(start = 6.dp, end = 6.dp, top = 28.dp, bottom = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun BaseItem.posterInfoText(): String? {
    if (type != BaseItemKind.MOVIE && type != BaseItemKind.SERIES) return null
    val genre = data.genres?.firstOrNull()
    val rating =
        data.communityRating
            ?.takeIf { it > 0 }
            ?.let { "★ " + String.format(Locale.getDefault(), "%.1f", it) }
    return listOfNotNull(genre, rating).joinToString(" · ").ifBlank { null }
}
