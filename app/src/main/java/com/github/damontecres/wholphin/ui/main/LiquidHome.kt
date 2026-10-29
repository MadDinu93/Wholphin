package com.github.damontecres.wholphin.ui.main

import java.util.EnumSet
import com.github.damontecres.wholphin.ui.util.LocalInterfaceCustomization
import com.github.damontecres.wholphin.ui.util.InterfaceCustomization
import com.github.damontecres.wholphin.preferences.DisplayToggle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.cards.CardPill
import com.github.damontecres.wholphin.ui.cards.FavoriteIndicator
import com.github.damontecres.wholphin.ui.cards.WatchedIcon
import com.github.damontecres.wholphin.ui.components.EpisodeName
import com.github.damontecres.wholphin.ui.components.ViewOptionImageType
import com.github.damontecres.wholphin.ui.components.QuickDetails
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.components.rememberLogoUrl
import com.github.damontecres.wholphin.ui.formatDuration
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.roundMinutes
import com.github.damontecres.wholphin.ui.timeRemaining
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.cardShape
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.MediaStreamType
import java.util.Locale

/** Height of [LiquidHomeHero] when shown below the top navigation bar */
val LiquidHeroHeight = 262.dp

private val Gold = Color(0xFFFFB873)

/**
 * "4K", "DV", "HDR" and "ATMOS" from the streams of the item, as far as the server sent them with the item. Nothing
 * is guessed, so an item without stream information has no badges.
 */
fun BaseItem.streamBadges(): List<String> {
    val streams = data.mediaStreams ?: data.mediaSources?.firstOrNull()?.mediaStreams ?: return emptyList()
    val video = streams.firstOrNull { it.type == MediaStreamType.VIDEO }
    return buildList {
        if ((video?.width ?: 0) >= 3800) add("4K")
        val range = video?.videoRangeType?.name.orEmpty()
        when {
            range == "HDR10_PLUS" -> add("HDR10+")
            range == "HDR10" || range == "HLG" -> add("HDR")
            range.startsWith("DOVI") -> add("DV")
        }
        val atmos =
            streams.any {
                it.type == MediaStreamType.AUDIO &&
                    (
                        it.displayTitle?.contains("atmos", ignoreCase = true) == true ||
                            it.profile?.contains("atmos", ignoreCase = true) == true
                    )
            }
        if (atmos) add("ATMOS")
    }
}

@Composable
private fun LiquidBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = LiquidColors.TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        maxLines = 1,
        modifier =
            modifier
                .background(Color.Black.copy(alpha = .65f), RoundedCornerShape(3.dp))
                .border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun HeroButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (primary) LiquidColors.TextPrimary else LiquidColors.Elevated,
                contentColor = if (primary) LiquidColors.Canvas else LiquidColors.TextPrimary,
                focusedContainerColor = LiquidColors.TextPrimary,
                focusedContentColor = LiquidColors.Canvas,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, LiquidColors.Rim), shape = CircleShape),
                focusedBorder = Border(BorderStroke(2.dp, LiquidColors.Accent), shape = CircleShape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * Scrolls the rows of the Modern and Squared home screens to the top with a slow, eased animation, so moving down to
 * another catalog reads as a scroll and not as a jump
 */
@OptIn(ExperimentalFoundationApi::class)
class LiquidBringIntoViewSpec(
    private val spaceAbovePx: Float,
) : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = tween(durationMillis = 550, easing = FastOutSlowInEasing)

    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float,
    ): Float = offset - spaceAbovePx
}

/**
 * The hero of the Modern and Squared home screens: the focused item's title,
 * details, badges and overview over a dark falloff, and a Play button. Only what Jellyfin has for the item
 * is shown, so nothing is left empty.
 */
@Composable
fun LiquidHomeHero(
    item: BaseItem?,
    showLogo: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // The text fades from one item to the next, instead of changing at once as the focus moves
        Crossfade(
            targetState = item,
            animationSpec = tween(300),
            label = "liquid_hero",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { shown ->
            LiquidHeroText(shown, showLogo)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            HeroButton(
                text = stringResource(R.string.play),
                icon = Icons.Default.PlayArrow,
                onClick = onPlay,
                primary = true,
            )
        }
    }
}

/** Title and genres at the top, the details and the overview at the bottom */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiquidHeroText(
    item: BaseItem?,
    showLogo: Boolean,
) {
    val title =
        remember(item?.id) {
            when (item?.type) {
                BaseItemKind.EPISODE,
                BaseItemKind.SEASON,
                -> item.data.seriesName

                else -> item?.title
            }
        }
    val subtitle =
        remember(item?.id) {
            when (item?.type) {
                BaseItemKind.SEASON,
                BaseItemKind.EPISODE,
                -> item.name

                else -> null
            }
        }
    val overview = item?.data?.overview
    val genres = item?.data?.genres?.take(3)?.joinToString(" / ")
    val badges = remember(item?.id) { item?.streamBadges().orEmpty() }
    // The age rating is left out of the hero, and nothing in it is wider than half of the screen
    val customization = LocalInterfaceCustomization.current
    val heroCustomization =
        remember(customization) {
            InterfaceCustomization(
                EnumSet.noneOf(DisplayToggle::class.java).apply {
                    addAll(customization.enabledDisplayToggles)
                    remove(DisplayToggle.OFFICIAL_RATING)
                },
            )
        }
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(.5f),
        ) {
            TitleOrLogo(
                title = title?.uppercase(),
                logoImageUrl = rememberLogoUrl(item),
                showLogo = showLogo,
                modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp),
            )
            if (subtitle != null) {
                EpisodeName(subtitle)
            }
            if (genres.isNotNullOrBlank()) {
                Text(
                    text = genres,
                    color = LiquidColors.Accent,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(.5f).padding(bottom = 8.dp),
        ) {
            CompositionLocalProvider(LocalInterfaceCustomization provides heroCustomization) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickDetails(
                        details = item?.ui?.quickDetails,
                        timeRemaining = item?.timeRemainingOrRuntime,
                        endsAt = item?.data?.endDate,
                        textStyle = MaterialTheme.typography.titleSmall,
                    )
                    badges.forEach { LiquidBadge(it) }
                }
            }
            val overviewModifier = Modifier.fillMaxWidth().height(80.dp)
            if (overview.isNotNullOrBlank()) {
                Text(
                    text = overview,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = LiquidColors.TextSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = overviewModifier,
                )
            } else {
                Spacer(overviewModifier)
            }
        }
    }
}

/** Card sizes of these styles are the configured height times this, the classic cards are bigger */
private const val PortraitScale = 1.1f
private const val LandscapeScale = .62f
private val PanelHeight = 56.dp

/**
 * A card of the Modern and Squared home screens. Modern: round cards, posters with the text under them. Squared:
 * almost square landscape cards with an information panel below the picture. Continue watching rows are landscape
 * with a progress bar and the time left in both. Items that are not titles (genres, studios) and the loading
 * placeholders keep the classic card.
 */
@Composable
fun LiquidHomeCard(
    style: HomeScreenStyle,
    index: Int,
    item: BaseItem?,
    isContinueRow: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    viewOptions: HomeRowViewOptions,
    modifier: Modifier,
) {
    if (item == null || item.type == BaseItemKind.GENRE || item.type == BaseItemKind.STUDIO) {
        HomePageCardContent(index, item, onClick, onLongClick, viewOptions, modifier)
        return
    }
    val squared = style == HomeScreenStyle.STYLE_SQUARED
    val shape = style.cardShape
    // The sizes, aspect ratio and image type of "Customize home page" apply, only scaled down for these styles. The
    // default tall ratio of continue watching rows, and of every row in Squared, becomes landscape.
    val isEpisode = item.type == BaseItemKind.EPISODE
    val configuredRatio = if (isEpisode) viewOptions.episodeAspectRatio.ratio else viewOptions.aspectRatio.ratio
    val landscape = configuredRatio >= 1f || isContinueRow || squared
    val ratio = if (configuredRatio >= 1f) configuredRatio else if (landscape) AspectRatios.WIDE else configuredRatio
    val imageHeight = viewOptions.heightDp.dp * (if (landscape) LandscapeScale else PortraitScale)
    val width = imageHeight * ratio
    val showTexts = viewOptions.showTitles

    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val context = LocalContext.current
    val timeLeft =
        remember(item, isContinueRow) {
            if (isContinueRow) {
                item.data.timeRemaining?.roundMinutes?.let {
                    context.resources.getString(R.string.time_left, context.resources.formatDuration(it))
                }
            } else {
                null
            }
        }
    val rating =
        remember(item) {
            item.data.communityRating
                ?.takeIf { it > 0 }
                ?.let { String.format(Locale.getDefault(), "%.1f", it) }
        }
    val title = item.title ?: item.name ?: ""
    val meta =
        remember(item, isContinueRow) {
            if (isContinueRow || item.type == BaseItemKind.EPISODE) {
                item.subtitle
            } else {
                listOfNotNull(
                    item.data.productionYear?.toString(),
                    // The rating is under the title, so the title has the whole width
                    rating?.let { "★ $it" },
                ).joinToString("  •  ").ifBlank { null }
            }
        }
    val badges = remember(item.id) { item.streamBadges() }
    val played = item.data.userData?.playedPercentage ?: 0.0

    val imageUrlService = LocalImageUrlService.current
    val density = LocalDensity.current
    val configuredType = if (isEpisode) viewOptions.episodeImageType else viewOptions.imageType
    val imageType =
        when {
            configuredType == ViewOptionImageType.THUMB -> ImageType.THUMB
            !landscape -> ImageType.PRIMARY
            isEpisode -> ImageType.PRIMARY
            else -> ImageType.BACKDROP
        }
    val imageUrl =
        remember(item, imageHeight, imageType) {
            item.imageUrlOverride
                ?: imageUrlService.getItemImageUrl(
                    item,
                    imageType,
                    fillWidth = null,
                    fillHeight = with(density) { imageHeight.roundToPx() },
                    useSeriesForPrimary = !(landscape && item.type == BaseItemKind.EPISODE),
                )
        }
    var imageError by remember(imageUrl) { mutableStateOf(false) }

    val picture: @Composable () -> Unit = {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(LiquidColors.Elevated),
        ) {
            if (!imageError && imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = title,
                    color = LiquidColors.TextSecondary,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                )
            }
            // The watched mark, favorite and the number of unwatched episodes, like the classic cards
            val hasProgress = played > 0.0 && played < 100.0
            val watched = item.data.userData?.played == true && !hasProgress
            val favorite = item.favorite
            val unplayedText = item.ui.episodeUnplayedCornerText
            if (watched || favorite || unplayedText != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                ) {
                    if (watched) WatchedIcon(Modifier.size(22.dp), padding = 3.dp)
                    if (favorite) FavoriteIndicator(fontSize = 16.sp)
                    if (unplayedText != null) CardPill(unplayedText)
                }
            }
            if (badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                ) {
                    badges.take(2).forEach { LiquidBadge(it) }
                }
            }
            if (played > 0.0 && played < 100.0) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(if (focused) 6.dp else 4.dp)
                            .background(Color.White.copy(alpha = .2f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((played / 100.0).toFloat().coerceIn(0f, 1f))
                            .background(LiquidColors.Accent),
                    )
                }
            }
        }
    }

    val texts: @Composable (Modifier) -> Unit = { textModifier ->
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = textModifier) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = LiquidColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val end = timeLeft
                if (end != null) {
                    Text(
                        text = end,
                        color = LiquidColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            if (meta != null) {
                Text(
                    text = meta,
                    color = LiquidColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    val cardBorder =
        CardDefaults.border(
            border = Border(BorderStroke(1.dp, LiquidColors.Rim), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, LiquidColors.TextPrimary), shape = shape),
        )
    if (squared) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            interactionSource = interactionSource,
            shape = CardDefaults.shape(shape),
            border = cardBorder,
            scale = CardDefaults.scale(focusedScale = 1.05f),
            colors = CardDefaults.colors(containerColor = LiquidColors.Base),
            modifier = modifier.width(width),
        ) {
            picture()
            if (showTexts) {
                texts(
                    Modifier
                        .fillMaxWidth()
                        .height(if (isContinueRow) 56.dp else PanelHeight)
                        .background(LiquidColors.Elevated)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.width(width),
        ) {
            Card(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                shape = CardDefaults.shape(shape),
                border = cardBorder,
                scale = CardDefaults.scale(focusedScale = 1.06f),
                colors = CardDefaults.colors(containerColor = LiquidColors.Elevated),
            ) {
                picture()
            }
            if (showTexts) texts(Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        }
    }
}
