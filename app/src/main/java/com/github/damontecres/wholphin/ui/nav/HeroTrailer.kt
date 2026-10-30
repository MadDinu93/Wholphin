package com.github.damontecres.wholphin.ui.nav

import android.graphics.Bitmap
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
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
    // Thorphin: on pause, not stop, as a TV input switch may not stop the activity and the sound would play on
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
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
                    }.clipToBounds(),
        ) { trailer ->
            if (trailer != null) {
                var picture by remember(trailer) { mutableStateOf(PictureBounds()) }
                // Moves smoothly when more of the picture shows up
                val left by animateFloatAsState(picture.left, tween(400), label = "picture_left")
                val top by animateFloatAsState(picture.top, tween(400), label = "picture_top")
                val right by animateFloatAsState(picture.right, tween(400), label = "picture_right")
                val bottom by animateFloatAsState(picture.bottom, tween(400), label = "picture_bottom")
                TrailerPlayer(
                    trailer = trailer,
                    heroTrailerService = heroTrailerService,
                    muted = muted,
                    onPicture = { picture = it },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                // The edges of the picture fade, not those of the video, which often has black bars
                                // in it, otherwise the picture would end on a hard line where the bars start
                                drawContent()
                                val startBar = if (isRtl) 1f - right else left
                                val endBar = if (isRtl) left else 1f - right
                                val pictureStart = startBar
                                val pictureEnd = 1f - endBar
                                // The far edge is the screen's, only faded when the picture ends before it
                                val endFade = (endBar * 4).coerceAtMost(.1f)
                                drawRect(
                                    brush =
                                        Brush.horizontalGradient(
                                            colorStops =
                                                fadeStops(pictureStart, pictureStart + (pictureEnd - pictureStart) * .4f) +
                                                    fadeStops(pictureEnd, pictureEnd - endFade).reversedArray(),
                                            startX = if (isRtl) size.width else 0f,
                                            endX = if (isRtl) 0f else size.width,
                                        ),
                                    blendMode = BlendMode.DstIn,
                                )
                                val height = bottom - top
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colorStops =
                                                fadeStops(top, top + height * .25f) +
                                                    fadeStops(bottom, bottom - height * .3f).reversedArray(),
                                        ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                )
            }
        }
    }
}

/**
 * Where the picture is inside the video, without the black bars many trailers have in the video itself, as fractions
 * of its size
 */
@Immutable
data class PictureBounds(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
)

/**
 * Color stops going from transparent at [from] to opaque at [to], eased so that neither end shows as a line
 */
private fun fadeStops(
    from: Float,
    to: Float,
): Array<Pair<Float, Color>> =
    Array(9) { i ->
        val t = i / 8f
        (from + (to - from) * t) to Color.Black.copy(alpha = t * t * (3 - 2 * t))
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
 * Plays the theme music of the focused card [item] on pages whose cards have no trailers, such as search, shortly
 * after it is focused, if theme music in cards is on
 */
@Composable
fun FocusedCardThemeMusic(
    item: BaseItem?,
    preferences: UserPreferences,
    viewModel: ApplicationContentViewModel = hiltViewModel(),
) {
    if (!preferences.appPreferences.interfacePreferences.cardThemeMusic) return
    val heroTrailerService = viewModel.heroTrailerService
    val owner = remember { Any() }
    LaunchedEffect(item?.id) {
        heroTrailerService.focus(item, owner, trailers = false, themeMusic = true)
    }
    DisposableEffect(Unit) { onDispose { heroTrailerService.focus(null, owner) } }
}

/**
 * Plays [trailer] filling its box with the picture of the video, zoomed in just enough to push the black bars many
 * trailers have in the video itself out of view; a video without bars is only fitted to the box
 */
@Composable
fun ZoomedTrailerPlayer(
    trailer: HeroTrailerSource,
    heroTrailerService: HeroTrailerService,
    muted: Boolean,
    modifier: Modifier = Modifier,
) {
    var picture by remember(trailer) { mutableStateOf(PictureBounds()) }
    // The first picture found is where the zoom starts, only later growth of the picture moves smoothly
    var first by remember(trailer) { mutableStateOf<PictureBounds?>(null) }
    val spec: AnimationSpec<Float> = if (first == null || picture == first) snap() else tween(400)
    val left by animateFloatAsState(picture.left, spec, label = "zoom_left")
    val top by animateFloatAsState(picture.top, spec, label = "zoom_top")
    val right by animateFloatAsState(picture.right, spec, label = "zoom_right")
    val bottom by animateFloatAsState(picture.bottom, spec, label = "zoom_bottom")
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val boxWidth = constraints.maxWidth
        val boxHeight = constraints.maxHeight
        TrailerPlayer(
            trailer = trailer,
            heroTrailerService = heroTrailerService,
            muted = muted,
            onPicture = {
                if (first == null) first = it
                picture = it
            },
            modifier =
                Modifier.layout { measurable, _ ->
                    // The video is 16:9, scaled so its picture covers the box and centered on that picture
                    val videoWidth = boxWidth.toFloat()
                    val videoHeight = videoWidth / AspectRatios.WIDE
                    val pictureWidth = ((right - left) * videoWidth).coerceAtLeast(1f)
                    val pictureHeight = ((bottom - top) * videoHeight).coerceAtLeast(1f)
                    val scale = max(boxWidth / pictureWidth, boxHeight / pictureHeight)
                    val width = videoWidth * scale
                    val height = videoHeight * scale
                    val placeable = measurable.measure(Constraints.fixed(width.roundToInt(), height.roundToInt()))
                    layout(boxWidth, boxHeight) {
                        val x = (boxWidth - width) / 2 + (.5f - (left + right) / 2) * width
                        val y = (boxHeight - height) / 2 + (.5f - (top + bottom) / 2) * height
                        placeable.place(x.roundToInt(), y.roundToInt())
                    }
                },
        )
    }
}

/**
 * Plays [trailer], fading in once the video is playing, and tells the [heroTrailerService] when it plays or ends
 *
 * With [onPicture], it is told where the picture is in the video, and the video only shows while it is not black,
 * such as between scenes, so the backdrop stays until the picture starts
 */
@Composable
fun TrailerPlayer(
    trailer: HeroTrailerSource,
    heroTrailerService: HeroTrailerService,
    muted: Boolean,
    modifier: Modifier = Modifier,
    onPicture: ((PictureBounds) -> Unit)? = null,
) {
    var playing by remember(trailer) { mutableStateOf(false) }
    // Always, unless looking for the picture
    var pictureShown by remember(trailer) { mutableStateOf(onPicture == null) }
    val shown = playing && pictureShown
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(if (shown) 800 else 500), label = "trailer_alpha")
    LaunchedEffect(shown) {
        if (shown) heroTrailerService.onPlaying(trailer.itemId)
    }
    val onEnded = { heroTrailerService.finished(trailer.itemId) }
    val onPlaying = { playing = true }
    val onFrame: ((PictureBounds?) -> Unit)? =
        onPicture?.let { onPicture ->
            { picture ->
                pictureShown = picture != null
                picture?.let(onPicture)
            }
        }
    LaunchedEffect(trailer) {
        // Give up if it never starts, e.g. no network or an embed that is not allowed
        delay(15.seconds)
        if (!playing) onEnded()
    }
    // On pause, not stop, as a TV input switch may not stop the activity and the sound would play on
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
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
                onFrame = onFrame,
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
                onFrame = onFrame,
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
    onFrame: ((PictureBounds?) -> Unit)?,
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
    if (onFrame == null) {
        // A texture view, unlike a surface view, can be faded like the backdrop image
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = modifier,
        )
        return
    }
    // Its own texture view, to look at the frames
    val context = LocalContext.current
    val textureView = remember(player) { TextureView(context).also { player.setVideoTextureView(it) } }
    val currentOnFrame by rememberUpdatedState(onFrame)
    LaunchedEffect(textureView) {
        val bitmap = Bitmap.createBitmap(SAMPLE_WIDTH, SAMPLE_HEIGHT, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(SAMPLE_WIDTH * SAMPLE_HEIGHT)
        // The bars are what stayed black in every frame so far, a dark scene alone could be taken for them
        var picture: PictureBounds? = null
        var blackFrames = 0
        try {
            while (true) {
                delay(250.milliseconds)
                if (!textureView.isAvailable || !player.isPlaying) continue
                textureView.getBitmap(bitmap)
                bitmap.getPixels(pixels, 0, SAMPLE_WIDTH, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT)
                val frame = findPicture(pixels)
                if (frame == null) {
                    // Not for a cut between scenes
                    if (++blackFrames == 2) currentOnFrame(null)
                } else {
                    blackFrames = 0
                    picture =
                        picture?.let {
                            PictureBounds(
                                left = min(it.left, frame.left),
                                top = min(it.top, frame.top),
                                right = max(it.right, frame.right),
                                bottom = max(it.bottom, frame.bottom),
                            )
                        } ?: frame
                    currentOnFrame(picture)
                }
            }
        } finally {
            bitmap.recycle()
        }
    }
    key(textureView) {
        AndroidView(factory = { textureView }, modifier = modifier)
    }
}

private const val SAMPLE_WIDTH = 128
private const val SAMPLE_HEIGHT = 72

/** No more than this much of each side is taken for a black bar */
private const val MAX_BAR = .2f

/**
 * Where the lit pixels are in the frame's [pixels], null when it is black
 */
private fun findPicture(pixels: IntArray): PictureBounds? {
    val litInRow = IntArray(SAMPLE_HEIGHT)
    val litInColumn = IntArray(SAMPLE_WIDTH)
    var lit = 0
    for (y in 0 until SAMPLE_HEIGHT) {
        for (x in 0 until SAMPLE_WIDTH) {
            val p = pixels[y * SAMPLE_WIDTH + x]
            val luma = (3 * (p shr 16 and 0xff) + 6 * (p shr 8 and 0xff) + (p and 0xff)) / 10
            // Above the noise of the black bars
            if (luma > 24) {
                litInRow[y]++
                litInColumn[x]++
                lit++
            }
        }
    }
    if (lit < SAMPLE_WIDTH * SAMPLE_HEIGHT / 100) return null
    // A couple of lit pixels in a row or column is noise
    val top = litInRow.indexOfFirst { it > 1 }
    val bottom = litInRow.indexOfLast { it > 1 }
    val left = litInColumn.indexOfFirst { it > 1 }
    val right = litInColumn.indexOfLast { it > 1 }
    if (top < 0 || left < 0) return null
    return PictureBounds(
        left = (left.toFloat() / SAMPLE_WIDTH).coerceAtMost(MAX_BAR),
        top = (top.toFloat() / SAMPLE_HEIGHT).coerceAtMost(MAX_BAR),
        right = ((right + 1f) / SAMPLE_WIDTH).coerceAtLeast(1 - MAX_BAR),
        bottom = ((bottom + 1f) / SAMPLE_HEIGHT).coerceAtLeast(1 - MAX_BAR),
    )
}
