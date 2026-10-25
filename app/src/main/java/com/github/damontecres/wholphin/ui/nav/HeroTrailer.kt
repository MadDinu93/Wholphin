package com.github.damontecres.wholphin.ui.nav

import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.HeroTrailerSource
import com.github.damontecres.wholphin.ui.AspectRatios
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Where the trailer should end on screen, so it does not reach the cards below the hero
 */
@Stable
class HeroTrailerArea {
    /** Where the trailer's bottom goes in root coordinates, 0 while no hero is shown */
    var bottom by mutableFloatStateOf(0f)
}

/**
 * Provided by [ApplicationContent] for the page showing the hero to report its [HeroTrailerArea]
 */
val LocalHeroTrailerArea = compositionLocalOf<HeroTrailerArea?> { null }

/**
 * Plays the [HeroTrailerService]'s current trailer where the [Backdrop] image is, fading in once the video is playing
 */
@Composable
fun HeroTrailerBackdrop(
    heroTrailerService: HeroTrailerService,
    area: HeroTrailerArea,
    muted: Boolean,
    modifier: Modifier = Modifier,
) {
    val current by heroTrailerService.current.collectAsStateWithLifecycle()
    val inCard by heroTrailerService.inCard.collectAsStateWithLifecycle()
    // Otherwise the focused card plays it, see CardTrailers
    val source = current.takeUnless { inCard }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        heroTrailerService.focus(null)
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = source,
            animationSpec = tween(600),
            label = "hero_trailer",
            // Where the backdrop image is with the same fades, so the video takes its place, but ending level with
            // the first row's title so it does not reach the cards, and 16:9 so the video is shown whole, neither cropped nor
            // stretched
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    // Read in the layout phase, the hero moves with the top bar
                    .layout { measurable, constraints ->
                        val maxHeight = constraints.maxHeight * .7f
                        val height = area.bottom.takeIf { it > 0f }?.coerceAtMost(maxHeight) ?: maxHeight
                        // It may grow to the left over the hero's text, which stays readable thanks to the faded edge
                        val width = min(height * AspectRatios.WIDE, constraints.maxWidth * .8f)
                        val placeable =
                            measurable.measure(
                                Constraints.fixed(width.roundToInt(), (width / AspectRatios.WIDE).roundToInt()),
                            )
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }.clipToBounds()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        // Only the edges fade, the rest of the video stays opaque. The top one also hides the black
                        // bars many trailers have in the video itself
                        val start = if (isRtl) size.width else 0f
                        val end = if (isRtl) 0f else size.width
                        drawContent()
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    colorStops = arrayOf(0f to Color.Transparent, .35f to Color.Black),
                                    startX = start,
                                    endX = end,
                                ),
                            blendMode = BlendMode.DstIn,
                        )
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    colorStops =
                                        arrayOf(
                                            0f to Color.Transparent,
                                            .12f to Color.Black,
                                            .7f to Color.Black,
                                            1f to Color.Transparent,
                                        ),
                                ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
        ) { trailer ->
            if (trailer != null) {
                TrailerPlayer(
                    trailer = trailer,
                    heroTrailerService = heroTrailerService,
                    muted = muted,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Trailers play inside the focused card, which grows to 16:9 for it, instead of behind the hero
 *
 * Provided by the pages with rows of cards when that is enabled, see [rememberCardTrailer]
 */
@Stable
class CardTrailers(
    val heroTrailerService: HeroTrailerService,
    val current: State<HeroTrailerSource?>,
    val inCard: State<Boolean>,
    val playing: State<Boolean>,
    val muted: Boolean,
)

val LocalCardTrailers = staticCompositionLocalOf<CardTrailers?> { null }

@Composable
fun rememberCardTrailers(
    heroTrailerService: HeroTrailerService,
    muted: Boolean,
): CardTrailers {
    val current = heroTrailerService.current.collectAsStateWithLifecycle()
    val inCard = heroTrailerService.inCard.collectAsStateWithLifecycle()
    val playing = heroTrailerService.playing.collectAsStateWithLifecycle()
    return remember(heroTrailerService, current, inCard, playing, muted) {
        CardTrailers(heroTrailerService, current, inCard, playing, muted)
    }
}

/**
 * The trailer to play in the card for [itemId], null unless it is that card's turn
 *
 * Only the card whose trailer it is recomposes when the trailer changes
 */
@Composable
fun rememberCardTrailer(itemId: UUID?): State<HeroTrailerSource?> {
    val cardTrailers = LocalCardTrailers.current
    return remember(cardTrailers, itemId) {
        derivedStateOf {
            cardTrailers
                ?.takeIf { it.inCard.value }
                ?.current
                ?.value
                ?.takeIf { it.itemId == itemId }
        }
    }
}

/**
 * Plays the trailer of [item] behind its details page, where the hero's would be, after the same delay
 *
 * Its sound takes over from the theme music.
 */
@Composable
fun DetailsTrailer(
    item: BaseItem?,
    preferences: UserPreferences,
    viewModel: ApplicationContentViewModel = hiltViewModel(),
) {
    val interfacePreferences = preferences.appPreferences.interfacePreferences
    // Either trailer setting means trailers are wanted
    if (interfacePreferences.disableHeroTrailer && !interfacePreferences.cardTrailers) return
    val heroTrailerService = viewModel.heroTrailerService
    val owner = remember { Any() }
    LaunchedEffect(item?.id) { heroTrailerService.focus(item, owner) }
    DisposableEffect(Unit) { onDispose { heroTrailerService.focus(null, owner) } }
    if (!interfacePreferences.muteHeroTrailer) {
        val playing by heroTrailerService.playing.collectAsStateWithLifecycle()
        LaunchedEffect(playing) {
            if (playing) viewModel.themeSongPlayer.stop()
        }
    }
}

/**
 * Plays [trailer], fading in once the video is playing, and tells the [heroTrailerService] when it plays or ends
 */
@Composable
fun TrailerPlayer(
    trailer: HeroTrailerSource,
    heroTrailerService: HeroTrailerService,
    muted: Boolean,
    modifier: Modifier = Modifier,
) {
    var playing by remember(trailer) { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (playing) 1f else 0f, tween(800), label = "trailer_alpha")
    val onEnded = { heroTrailerService.finished(trailer.itemId) }
    val onPlaying = {
        playing = true
        heroTrailerService.onPlaying(trailer.itemId)
    }
    LaunchedEffect(trailer) {
        // Give up if it never starts, e.g. no network or an embed that is not allowed
        delay(15.seconds)
        if (!playing) onEnded()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // Not in the background
        heroTrailerService.focus(null)
    }
    val playerModifier = modifier.graphicsLayer { this.alpha = alpha }
    when (trailer) {
        is HeroTrailerSource.Local -> {
            TrailerVideoPlayer(
                heroTrailerService = heroTrailerService,
                url = trailer.url,
                fromServer = true,
                muted = muted,
                onPlaying = onPlaying,
                onEnded = onEnded,
                modifier = playerModifier,
            )
        }

        is HeroTrailerSource.Direct -> {
            TrailerVideoPlayer(
                heroTrailerService = heroTrailerService,
                url = trailer.url,
                fromServer = false,
                muted = muted,
                onPlaying = onPlaying,
                onEnded = onEnded,
                modifier = playerModifier,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun TrailerVideoPlayer(
    heroTrailerService: HeroTrailerService,
    url: String,
    fromServer: Boolean,
    muted: Boolean,
    onPlaying: () -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = remember(url) { heroTrailerService.createPlayer(fromServer) }
    val currentOnPlaying by rememberUpdatedState(onPlaying)
    val currentOnEnded by rememberUpdatedState(onEnded)
    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) currentOnPlaying()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) currentOnEnded()
                }

                override fun onPlayerError(error: PlaybackException) {
                    currentOnEnded()
                }
            }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    LaunchedEffect(player, muted) {
        player.volume = if (muted) 0f else 1f
    }
    // A texture view, unlike a surface view, can be faded like the backdrop image
    PlayerSurface(
        player = player,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        modifier = modifier,
    )
}
