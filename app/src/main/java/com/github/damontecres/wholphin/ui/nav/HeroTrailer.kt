package com.github.damontecres.wholphin.ui.nav

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.HeroTrailerSource
import com.github.damontecres.wholphin.ui.AspectRatios
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/**
 * Where the hero ends on screen, so the trailer can stay above the rows below it
 */
@Stable
class HeroTrailerArea {
    /** Bottom of the hero in root coordinates, 0 while no hero is shown */
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
    val source by heroTrailerService.current.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        heroTrailerService.focus(null)
    }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = source,
            animationSpec = tween(600),
            label = "hero_trailer",
            // Where the backdrop image is with the same fades, so the video takes its place, but only as tall as
            // the hero so it does not reach the rows, and 16:9 so the video is shown whole, neither cropped nor
            // stretched
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    // Read in the layout phase, the hero moves with the top bar
                    .layout { measurable, constraints ->
                        val maxHeight = constraints.maxHeight * .7f
                        val height = area.bottom.takeIf { it > 0f }?.coerceAtMost(maxHeight) ?: maxHeight
                        val width = min(height * AspectRatios.WIDE, constraints.maxWidth * .7f)
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
                var playing by remember(trailer) { mutableStateOf(false) }
                val alpha by animateFloatAsState(if (playing) 1f else 0f, tween(800), label = "hero_trailer_alpha")
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
                val playerModifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }
                when (trailer) {
                    is HeroTrailerSource.Local -> {
                        LocalTrailerPlayer(
                            heroTrailerService = heroTrailerService,
                            url = trailer.url,
                            muted = muted,
                            onPlaying = onPlaying,
                            onEnded = onEnded,
                            modifier = playerModifier,
                        )
                    }

                    is HeroTrailerSource.YouTube -> {
                        YouTubeTrailerPlayer(
                            videoId = trailer.videoId,
                            muted = muted,
                            onPlaying = onPlaying,
                            onEnded = onEnded,
                            modifier = playerModifier,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LocalTrailerPlayer(
    heroTrailerService: HeroTrailerService,
    url: String,
    muted: Boolean,
    onPlaying: () -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = remember(url) { heroTrailerService.createPlayer() }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeTrailerPlayer(
    videoId: String,
    muted: Boolean,
    onPlaying: () -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPlaying by rememberUpdatedState(onPlaying)
    val currentOnEnded by rememberUpdatedState(onEnded)
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                // Only a picture, the D-pad must stay on the rows
                isFocusable = false
                isFocusableInTouchMode = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onReveal() {
                            post { currentOnPlaying() }
                        }

                        @JavascriptInterface
                        fun onEnded() {
                            post { currentOnEnded() }
                        }
                    },
                    "Thorphin",
                )
                val script = youTubeCleanupScript(muted)
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView,
                            url: String?,
                        ) {
                            view.evaluateJavascript(script, null)
                        }
                    }
                // The embed page is loaded directly rather than in an iframe, so its player UI can be hidden.
                // YouTube refuses embeds (error 152) unless the referrer identifies the app.
                val origin = "https://" + context.packageName
                loadUrl(youTubeEmbedUrl(videoId, muted, origin), mapOf("Referer" to "$origin/"))
            }
        },
        onRelease = {
            it.stopLoading()
            it.destroy()
        },
        modifier = modifier,
    )
}

private fun youTubeEmbedUrl(
    videoId: String,
    muted: Boolean,
    origin: String,
) = "https://www.youtube.com/embed/$videoId?autoplay=1&mute=${if (muted) 1 else 0}&controls=0&disablekb=1&fs=0" +
    "&iv_load_policy=3&modestbranding=1&playsinline=1&rel=0&cc_load_policy=0&origin=${Uri.encode(origin)}"

/**
 * Hides every part of the YouTube player UI (play button, play/pause icon, title, logo, spinner, end screen) and
 * drives the video element directly: the app is told to show it on the first frame and to go back to the image just
 * before the end, so the suggestions never appear.
 */
private fun youTubeCleanupScript(muted: Boolean): String =
    """
    (function() {
      if (window.__thorphin) return;
      window.__thorphin = true;
      // YouTube renames its player classes, so instead of hiding them one by one everything is made invisible
      // and only the video element is shown again (visibility, unlike display, can be undone by a child)
      var style = document.createElement('style');
      style.textContent =
        '* { visibility: hidden !important; }' +
        // The player places the video with offsets of its own, pinned here so it fills the frame exactly
        'video { visibility: visible !important; object-fit: cover !important; position: fixed !important;' +
        ' top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important;' +
        ' margin: 0 !important; transform: none !important; }' +
        'html, body { background: transparent !important; }';
      function keepStyle() {
        if (!style.isConnected) (document.head || document.documentElement).appendChild(style);
      }
      keepStyle();
      setInterval(keepStyle, 250);
      var tries = 0;
      function hook() {
        var video = document.querySelector('video');
        if (!video) {
          if (document.querySelector('.ytp-error') || ++tries > 100) {
            console.log('HeroTrailer no video');
            Thorphin.onEnded();
          } else {
            setTimeout(hook, 100);
          }
          return;
        }
        var revealed = false;
        var ended = false;
        video.muted = $muted;
        if (!$muted) video.volume = 1;
        video.addEventListener('timeupdate', function() {
          if (!revealed && video.currentTime > 0.1) {
            revealed = true;
            console.log('HeroTrailer reveal');
            Thorphin.onReveal();
          }
          if (!ended && video.duration > 0 && video.currentTime >= video.duration - 1) {
            ended = true;
            Thorphin.onEnded();
          }
        });
        video.addEventListener('ended', function() {
          if (!ended) { ended = true; Thorphin.onEnded(); }
        });
        var started = video.play();
        if (started && started.catch) {
          started.catch(function(e) {
            console.log('HeroTrailer play failed ' + e);
            video.muted = true;
            video.play();
          });
        }
      }
      hook();
    })();
    """.trimIndent()
