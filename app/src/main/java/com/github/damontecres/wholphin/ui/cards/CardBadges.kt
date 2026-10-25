package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.CardProgressStyle
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.roundMinutes
import com.github.damontecres.wholphin.ui.timeRemaining

/**
 * How cards show how far into an item the user is, from the interface preferences
 */
val LocalCardProgressStyle = compositionLocalOf { CardProgressStyle.PROGRESS_TOP_BAR }

/**
 * Whether posters show their genre and rating, from the interface preferences
 */
val LocalShowCardInfo = compositionLocalOf { true }

private val TrackColor = Color.White.copy(alpha = .3f)
private val FillColor = Color.White.copy(alpha = .95f)
private val PillColor = Color.Black.copy(alpha = .55f)
private val CompactBoxColor = Color.Black.copy(alpha = .75f)
private val WatchedTabColor = Color.White.copy(alpha = .3f)
private val TopScrim = Brush.verticalGradient(listOf(Color.Black.copy(alpha = .7f), Color.Transparent))

/**
 * Whether [percent] is partly through, so there is progress to show
 */
fun hasCardProgress(percent: Double?) = percent != null && percent > 0 && percent < 100

/**
 * Everything a card shows over its image, kept to the top so the bottom of posters stays clear: many posters have
 * ratings drawn along the bottom
 *
 * From the top: the progress bar, a line with the favorite, the time left or watched mark and [cornerText], then the
 * genre and rating when [showInfo].
 *
 * @param cornerText such as the episode number or the number of unwatched episodes
 * @param versions the number of versions of the item, shown when more than one
 */
@Composable
fun BoxScope.CardTopOverlay(
    item: BaseItem?,
    percent: Double?,
    played: Boolean,
    favorite: Boolean,
    cornerText: String?,
    showInfo: Boolean,
    versions: Int = 0,
) {
    val hasProgress = hasCardProgress(percent)
    val style = LocalCardProgressStyle.current
    val barAtTop = hasProgress && style == CardProgressStyle.PROGRESS_TOP_BAR
    val progressInCorner = hasProgress && style == CardProgressStyle.PROGRESS_TOP_COMPACT
    val resources = LocalResources.current
    val info =
        if (showInfo && LocalShowCardInfo.current) {
            remember(item) { item?.posterInfoText() }
        } else {
            null
        }
    val timeLeft =
        if (barAtTop) {
            remember(item, resources) {
                item
                    ?.data
                    ?.timeRemaining
                    ?.roundMinutes
                    ?.let { resources.getString(R.string.time_left, resources.formatDuration(it)) }
            }
        } else {
            null
        }
    // With the bar along the top, the corner text joins the time left in the middle
    val centerText =
        listOfNotNull(cornerText.takeIf { barAtTop }, timeLeft).joinToString(" · ").ifBlank { null }
    val endText = cornerText.takeUnless { barAtTop }
    val watched = played && !hasProgress
    val hasBadges =
        watched || progressInCorner || favorite || versions > 1 || centerText != null || endText != null
    if (!hasBadges && !barAtTop && info == null) return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .then(
                    // Only behind text, so a lone badge does not darken the poster
                    if (barAtTop || info != null || centerText != null) Modifier.background(TopScrim) else Modifier,
                ),
    ) {
        if (barAtTop) {
            ProgressTrack(
                percent!!,
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                    .height(4.dp),
            )
        }
        if (hasBadges) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                ) {
                    if (progressInCorner) {
                        Box(
                            modifier =
                                Modifier
                                    .background(CompactBoxColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 5.dp),
                        ) {
                            ProgressTrack(percent!!, Modifier.width(44.dp).height(3.dp))
                        }
                    }
                    if (versions > 1) {
                        CardPill(versions.toString())
                    }
                    if (favorite) {
                        FavoriteIndicator(fontSize = 16.sp)
                    }
                }
                if (watched) {
                    WatchedTab(Modifier.align(Alignment.TopCenter))
                } else if (centerText != null) {
                    CardPill(centerText, Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
                }
                if (endText != null) {
                    CardPill(endText, Modifier.align(Alignment.TopEnd).padding(6.dp))
                }
            }
        }
        if (info != null) {
            Text(
                text = info,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = if (hasBadges) 2.dp else 10.dp),
            )
        }
        if (barAtTop || info != null || centerText != null) {
            // Where the scrim fades out
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProgressTrack(
    percent: Double,
    modifier: Modifier,
) {
    Box(modifier = modifier.background(TrackColor, CircleShape)) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent / 100).toFloat().coerceIn(0f, 1f))
                    .background(FillColor, CircleShape),
        )
    }
}

/**
 * Small dark frosted label on a card, such as the time left
 */
@Composable
fun CardPill(
    text: String,
    modifier: Modifier = Modifier,
) = Text(
    text = text,
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.SemiBold,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier =
        modifier
            .background(PillColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
)

/**
 * The item has been watched: a frosted tab hanging from the middle of the card's top edge
 */
@Composable
private fun WatchedTab(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(width = 34.dp, height = 22.dp)
                .background(WatchedTabColor, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}
