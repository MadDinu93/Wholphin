package com.github.damontecres.wholphin.ui.detail

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.HeroTrailerSource
import com.github.damontecres.wholphin.ui.components.FilterByButton
import com.github.damontecres.wholphin.ui.components.QuickDetails
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.main.streamBadges
import com.github.damontecres.wholphin.ui.nav.TrailerPlayer
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.LocalHomeScreenStyle
import com.github.damontecres.wholphin.ui.tryRequestFocus
import kotlinx.coroutines.delay
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.Date

/**
 * The layout of Movies, Series and Home of the "Alternative UI Minimalist" design: the catalogues on the left, and next
 * to them the details of the focused title with its trailer on top and a compact list of the titles below. It takes
 * the same input as [AlternativeBrowseLayout], which hands over to it when this style is chosen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MinimalistBrowseLayout(
    heading: String,
    entries: List<BrowseEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<BaseItem?>,
    loading: Boolean,
    images: AlternativeBrowseViewModel,
    onOpen: (Int, BaseItem) -> Unit,
    onPlay: (BaseItem) -> Unit,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    filter: BrowseFilter? = null,
    showType: Boolean = false,
) {
    var focusedItem by remember { mutableStateOf<BaseItem?>(null) }
    val listFocus = remember { FocusRequester() }
    var focusedOnce by remember { mutableStateOf(false) }
    // The first time titles are there the page starts on the list, not on the catalogues
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isNotEmpty() && !focusedOnce) {
            focusedOnce = true
            delay(100)
            listFocus.tryRequestFocus()
        }
    }
    // Another catalogue starts with its first title
    LaunchedEffect(selected) { focusedItem = null }
    val shown = focusedItem ?: items.firstOrNull()

    // Same trailer handling as AlternativeBrowseLayout: "in card", so the backdrop of the whole app stays quiet
    val interfacePreferences = preferences.appPreferences.interfacePreferences
    val trailerService = images.heroTrailerService
    val trailersWanted = !interfacePreferences.disableHeroTrailer || interfacePreferences.cardTrailers
    val trailerOwner = remember { Any() }
    LaunchedEffect(shown?.id) {
        trailerService.focus(
            shown,
            trailerOwner,
            inCard = true,
            trailers = trailersWanted,
            themeMusic = interfacePreferences.cardThemeMusic,
        )
    }
    DisposableEffect(Unit) { onDispose { trailerService.focus(null, trailerOwner) } }
    val trailer by trailerService.current.collectAsStateWithLifecycle()

    var contentFocused by remember { mutableStateOf(false) }
    // Going back to the catalogues always lands on the one that is shown, not on the one next to the list
    val selectedCatalogue = remember { FocusRequester() }
    BackHandler(enabled = contentFocused) { selectedCatalogue.tryRequestFocus() }
    val textScale = (interfacePreferences.detailsTextScale.takeIf { it > 0 } ?: 100) / 100f
    // The Cards design shows the titles as posters in a row instead of lines of text
    val cards = LocalHomeScreenStyle.current == HomeScreenStyle.STYLE_ALTERNATIVE_MINIMALIST_CARDS
    val density = LocalConfiguration.current.densityDpi / 160f

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize().padding(bottom = 8.dp),
    ) {
        // Catalogues, always wide in this design
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .width(190.dp)
                    .fillMaxHeight()
                    .background(LiquidColors.Rail, RoundedCornerShape(12.dp))
                    .focusGroup()
                    .focusProperties { onEnter = { selectedCatalogue.tryRequestFocus() } }
                    .padding(10.dp),
        ) {
            Text(
                text = heading,
                color = LiquidColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    CatalogueRow(
                        title = entry.title,
                        glyph = entry.glyph,
                        collapsed = false,
                        selected = selected == index,
                        onFocused = { onSelect(index) },
                        onClick = { listFocus.tryRequestFocus() },
                        modifier = Modifier.ifElse(selected == index, Modifier.focusRequester(selectedCatalogue)),
                    )
                }
            }
        }
        // Details with the trailer on top, the list of titles below
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { contentFocused = it.hasFocus }
                    .background(LiquidColors.Base.copy(alpha = .5f), RoundedCornerShape(12.dp)),
        ) {
            Crossfade(
                targetState = shown,
                animationSpec = tween(250),
                label = "minimalist_details",
                modifier = Modifier.weight(if (cards) .5f else .56f).fillMaxWidth(),
            ) { item ->
                MinimalistShowcase(
                    images = images,
                    item = item,
                    backdropUrl = remember(item?.id) { images.backdropUrl(item, 1200) },
                    trailer = trailer?.takeIf { it.itemId == item?.id },
                    trailerService = trailerService,
                    muted = interfacePreferences.muteHeroTrailer,
                    textScale = textScale,
                    hideDetails = interfacePreferences.hideDetailsDuringTrailer,
                    onPlay = { item?.let(onPlay) },
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(if (cards) .5f else .44f)
                        .fillMaxWidth()
                        .background(LiquidColors.Rail.copy(alpha = .5f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                ) {
                    Text(
                        text = entries.getOrNull(selected)?.title.orEmpty().uppercase(),
                        color = LiquidColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.library_title_count, items.size, items.size),
                        color = LiquidColors.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (filter != null) {
                        FilterByButton(
                            filterOptions = filter.options,
                            current = filter.current,
                            onFilterChange = filter.onChange,
                            getPossibleValues = filter.getValues,
                            alwaysExpanded = true,
                        )
                    }
                }
                if (items.isNotEmpty() && cards) {
                    val widthPx = remember(density) { (100 * density).toInt() }
                    // Another catalogue starts at its first title, not where the previous one was scrolled to
                    LazyRow(
                        state = remember(selected) { LazyListState() },
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(count = items.size, key = { it }) { index ->
                            val item = items[index]
                            Box(modifier = Modifier.width(100.dp)) {
                                BrowsePoster(
                                    item = item,
                                    imageUrl = remember(item?.id) { images.posterUrl(item, widthPx * 2) },
                                    onClick = { item?.let { onOpen(index, it) } },
                                    onFocused = { item?.let { focusedItem = it } },
                                    modifier = if (index == 0) Modifier.focusRequester(listFocus) else Modifier,
                                )
                            }
                        }
                    }
                } else if (items.isNotEmpty()) {
                    LazyColumn(
                        state = remember(selected) { LazyListState() },
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(count = items.size, key = { it }) { index ->
                            val item = items[index]
                            MinimalistTitleRow(
                                images = images,
                                item = item,
                                onClick = { item?.let { onOpen(index, it) } },
                                onFocused = { item?.let { focusedItem = it } },
                                showType = showType,
                                modifier = if (index == 0) Modifier.focusRequester(listFocus) else Modifier,
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (loading) stringResource(R.string.loading) else stringResource(R.string.browse_empty),
                        color = LiquidColors.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

/**
 * A hero card: the trailer (or the backdrop) fills the whole section and the title, details, overview and the Play
 * button, which stays in the bottom left corner, are drawn over it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinimalistShowcase(
    images: AlternativeBrowseViewModel,
    item: BaseItem?,
    backdropUrl: String?,
    trailer: HeroTrailerSource?,
    trailerService: HeroTrailerService,
    muted: Boolean,
    textScale: Float,
    hideDetails: Boolean,
    onPlay: () -> Unit,
) {
    val labelStyle = MaterialTheme.typography.labelMedium.let { it.copy(fontSize = it.fontSize * textScale) }
    // The text steps back while a trailer plays and returns when it ends
    val trailerPlaying by trailerService.playing.collectAsStateWithLifecycle()
    val overlayAlpha by animateFloatAsState(
        if (trailer != null && trailerPlaying) if (hideDetails) 0f else .35f else 1f,
        tween(600),
        label = "overlay_alpha",
    )
    val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    Box(modifier = Modifier.fillMaxSize().clip(shape).background(LiquidColors.Rail, shape)) {
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (trailer != null) {
            // The video is 16:9 and covers the section, what does not fit is cut off
            BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val videoRatio = 16f / 9f
                val coverWidth = if (maxWidth / maxHeight > videoRatio) maxWidth else maxHeight * videoRatio
                TrailerPlayer(
                    trailer = trailer,
                    heroTrailerService = trailerService,
                    muted = muted,
                    modifier = Modifier.requiredSize(coverWidth, coverWidth / videoRatio),
                )
            }
        }
        // The text stays readable on the left, the picture shows on the right and fades into the list below
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = overlayAlpha }
                    .background(
                        Brush.horizontalGradient(
                            0f to LiquidColors.Rail.copy(alpha = .95f),
                            .55f to LiquidColors.Rail.copy(alpha = .7f),
                            1f to Color.Transparent,
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(.6f to Color.Transparent, 1f to LiquidColors.Rail.copy(alpha = .6f))),
        )
        if (item != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .fillMaxWidth(.62f)
                        .fillMaxHeight()
                        .graphicsLayer { alpha = overlayAlpha }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = (item.title ?: "").uppercase(),
                    color = LiquidColors.TextPrimary,
                    fontSize = 24.sp * textScale,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                QuickDetails(
                    details = item.ui.quickDetails,
                    timeRemaining = null,
                    textStyle = labelStyle,
                )
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    val context = LocalContext.current
                    val endsAt =
                        remember(item.id) {
                            item.timeRemainingOrRuntime?.let {
                                DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + it.inWholeMilliseconds))
                            }
                        }
                    if (endsAt != null) {
                        Text(text = stringResource(R.string.ends_at, endsAt), color = LiquidColors.TextPrimary, style = labelStyle)
                    }
                    item.streamBadges().forEach { badge -> MinimalistChip(badge, textScale, RoundedCornerShape(4.dp)) }
                    val genres by produceState(emptyList<String>(), item.id) { value = images.genres(item) }
                    genres.take(3).forEach { genre -> MinimalistChip(genre, textScale, CircleShape) }
                }
                // The overview takes all the room up to the button and is only cut where the last line would not fit
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val overview = item.data.overview
                    if (overview.isNotNullOrBlank()) {
                        val lineHeight = 17.sp * textScale
                        val lines = with(LocalDensity.current) { (maxHeight / lineHeight.toDp()).toInt() }.coerceAtLeast(1)
                        Text(
                            text = overview,
                            color = LiquidColors.TextPrimary.copy(alpha = .85f),
                            fontSize = 12.sp * textScale,
                            lineHeight = lineHeight,
                            maxLines = lines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                DetailsButton(
                    text = if (item.resumeMs > 0) stringResource(R.string.resume) else stringResource(R.string.play),
                    icon = Icons.Default.PlayArrow,
                    primary = true,
                    onClick = onPlay,
                )
            }
        }
    }
}

@Composable
private fun MinimalistChip(
    text: String,
    textScale: Float,
    shape: androidx.compose.ui.graphics.Shape,
) {
    Text(
        text = text,
        color = LiquidColors.TextPrimary,
        fontSize = 10.sp * textScale,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier.background(LiquidColors.Elevated, shape).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** One line of the list: the title with its year and badges, the rating at the end. Focusing it shows its details. */
@Composable
private fun MinimalistTitleRow(
    images: AlternativeBrowseViewModel,
    item: BaseItem?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    showType: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                // Drawn below with the border: the animated container color lags behind while scrolling
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(1.5.dp, LiquidColors.Accent), shape = shape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(if (focused) LiquidColors.Elevated else LiquidColors.Rail.copy(alpha = .6f), shape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (focused) LiquidColors.Accent else LiquidColors.TextSecondary.copy(alpha = .4f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = (item?.title ?: "").uppercase(),
                color = if (focused) LiquidColors.Accent else LiquidColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (focused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val badges = remember(item?.id) { item?.streamBadges().orEmpty().take(2) }
            val genres by produceState(emptyList<String>(), item?.id) { value = item?.let { images.genres(it) }.orEmpty() }
            // Home mixes movies and series in its rows, so there each title says which it is
            val type =
                when (item?.type) {
                    BaseItemKind.MOVIE -> stringResource(R.string.type_movie).takeIf { showType }
                    BaseItemKind.SERIES, BaseItemKind.SEASON, BaseItemKind.EPISODE -> stringResource(R.string.type_series).takeIf { showType }
                    else -> null
                }
            val meta = listOfNotNull(item?.data?.productionYear?.toString(), type) + genres.take(2) + badges
            Text(
                text = meta.joinToString(" • "),
                color = LiquidColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            val rating = item?.data?.communityRating
            if (rating != null && rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = LiquidColors.Accent, modifier = Modifier.size(10.dp))
                    Text(
                        text = "%.1f".format(rating),
                        color = if (focused) LiquidColors.Accent else LiquidColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
