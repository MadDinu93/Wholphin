package com.github.damontecres.wholphin.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.LocalTrailer
import com.github.damontecres.wholphin.data.model.RemoteTrailer
import com.github.damontecres.wholphin.services.hilt.AuthOkHttpClient
import com.github.damontecres.wholphin.services.hilt.IoCoroutineScope
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * A trailer to play behind the home page hero
 */
sealed interface HeroTrailerSource {
    /** The item whose hero is showing, not the trailer's own id */
    val itemId: UUID

    /** A trailer file on the Jellyfin server, played with ExoPlayer */
    data class Local(
        override val itemId: UUID,
        val url: String,
    ) : HeroTrailerSource

    /** A video from anywhere else, given as a link to the video itself, played with ExoPlayer */
    data class Direct(
        override val itemId: UUID,
        val url: String,
    ) : HeroTrailerSource
}

/**
 * Picks a trailer for the item focused in the hero once it has stayed focused for a while, Netflix style, and then its
 * theme music if wanted
 */
@Singleton
class HeroTrailerService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:AuthOkHttpClient private val authOkHttpClient: OkHttpClient,
        @param:StandardOkHttpClient private val standardOkHttpClient: OkHttpClient,
        private val api: ApiClient,
        private val trailerService: TrailerService,
        private val themeSongPlayer: ThemeSongPlayer,
        @param:IoCoroutineScope private val scope: CoroutineScope,
    ) {
        private val _current = MutableStateFlow<HeroTrailerSource?>(null)
        val current: StateFlow<HeroTrailerSource?> = _current

        private val _playing = MutableStateFlow(false)

        /** Whether the [current] trailer is on screen, so the backdrop image can make way for it */
        val playing: StateFlow<Boolean> = _playing

        private val _inCard = MutableStateFlow(false)

        /** Whether the [current] trailer plays inside the focused card rather than behind the hero */
        val inCard: StateFlow<Boolean> = _inCard

        // Keyed by the item that has the trailers, a null value means it has none we can play
        private val cache = ConcurrentHashMap<UUID, Optional>()
        private var job: Job? = null
        private var owner: Any? = null

        // The item whose theme music plays once the trailer is over, null if none is wanted
        @Volatile
        private var themeItemId: UUID? = null

        /**
         * The hero now shows [item], or nothing if null. Stops any trailer and starts the delay for the new one.
         *
         * With an [owner], such as a page, clearing it with a null [item] only happens if no one else has asked for a
         * trailer since. Pages starting and closing during navigation then do not stop each other's trailers.
         *
         * @param trailers whether to play a trailer, if not the theme music starts sooner
         * @param themeMusic whether to play the item's theme music after the trailer, or if it has none
         */
        fun focus(
            item: BaseItem?,
            owner: Any? = null,
            inCard: Boolean = false,
            trailers: Boolean = true,
            themeMusic: Boolean = false,
        ) {
            if (item == null && owner != null && owner !== this.owner) return
            this.owner = owner
            job?.cancel()
            _current.value = null
            _playing.value = false
            _inCard.value = inCard
            // Only the music started here, a details page plays its own
            themeSongPlayer.stop(this)
            themeItemId = item?.takeIf { themeMusic }?.themeItemId()
            if (item == null) return
            job =
                scope.launch {
                    if (!trailers) {
                        // Not while scrolling past
                        delay(THEME_MUSIC_DELAY)
                        playThemeMusic()
                        return@launch
                    }
                    delay(PREPARE_DELAY)
                    val source = resolve(item)
                    Timber.v("Hero trailer for %s: %s", item.id, source)
                    if (source == null) {
                        // Nothing to wait for
                        playThemeMusic()
                        return@launch
                    }
                    // The add-on takes seconds to start a trailer, it gets ready during the rest of the delay
                    (source as? HeroTrailerSource.Direct)?.let { prepare(it.url) }
                    delay(START_DELAY - PREPARE_DELAY)
                    _current.value = source
                    // Fetched while the trailer plays, for the same reason
                    themeItemId?.let { themeSongPlayer.prepareThemeFor(it, this@HeroTrailerService) }
                }
        }

        /**
         * Lets the server of [url] know it is about to be played with a HEAD request, which any server answers. The
         * Thorphin Trailers add-on gets the video ready then.
         */
        private fun prepare(url: String) {
            standardOkHttpClient
                .newCall(Request.Builder().url(url).head().build())
                .enqueue(
                    object : Callback {
                        override fun onFailure(
                            call: Call,
                            e: IOException,
                        ) {
                            Timber.v(e, "Could not prepare %s", url)
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response,
                        ) {
                            response.close()
                        }
                    },
                )
        }

        /** The trailer for [itemId] started playing */
        fun onPlaying(itemId: UUID) {
            if (_current.value?.itemId == itemId) {
                _playing.value = true
            }
        }

        /** The trailer for [itemId] ended or failed, go back to the image and on to the theme music */
        fun finished(itemId: UUID) {
            if (_current.value?.itemId == itemId) {
                _current.value = null
                _playing.value = false
                playThemeMusic()
            }
        }

        private fun playThemeMusic() {
            themeItemId?.let { themeSongPlayer.playThemeFor(it, this) }
            // Once, not again for the same focus
            themeItemId = null
        }

        private fun BaseItem.themeItemId(): UUID? =
            when (type) {
                BaseItemKind.MOVIE, BaseItemKind.SERIES, BaseItemKind.BOX_SET -> id
                BaseItemKind.EPISODE, BaseItemKind.SEASON -> data.seriesId
                else -> null
            }

        /**
         * @param fromServer whether the video is on the Jellyfin server, only then are the user's credentials sent
         */
        @OptIn(UnstableApi::class)
        fun createPlayer(fromServer: Boolean): ExoPlayer =
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        OkHttpDataSource.Factory(if (fromServer) authOkHttpClient else standardOkHttpClient),
                    ),
                ).build()

        private suspend fun resolve(item: BaseItem): HeroTrailerSource? {
            val trailerItemId =
                when (item.type) {
                    BaseItemKind.MOVIE, BaseItemKind.SERIES -> item.id
                    BaseItemKind.EPISODE, BaseItemKind.SEASON -> item.data.seriesId
                    else -> null
                } ?: return null
            cache[trailerItemId]?.let { return it.source?.withItemId(item.id) }
            val source =
                try {
                    // Rows do not fetch the trailer fields, so get the full item
                    val full = BaseItem(api.userLibraryApi.getItem(itemId = trailerItemId).content, false)
                    val local = trailerService.getLocalTrailers(full).firstOrNull() as? LocalTrailer
                    if (local != null) {
                        HeroTrailerSource.Local(
                            itemId = item.id,
                            url = api.videosApi.getVideoStreamUrl(itemId = local.baseItem.id, static = true),
                        )
                    } else {
                        val remoteUrls =
                            trailerService
                                .getRemoteTrailers(full)
                                .mapNotNull { (it as? RemoteTrailer)?.url?.unwrapTrailerUrl() }
                        // Only links to the video itself, such as from the Thorphin Trailers add-on. YouTube pages are
                        // not played, a link that turns out to be another web page fails to play and the image comes back.
                        remoteUrls
                            .firstOrNull { it.isVideoUrl() }
                            ?.let { HeroTrailerSource.Direct(itemId = item.id, url = it) }
                    }
                } catch (ex: Exception) {
                    Timber.w(ex, "Could not get trailers for %s", trailerItemId)
                    return null
                }
            cache[trailerItemId] = Optional(source)
            return source
        }

        private fun HeroTrailerSource.withItemId(itemId: UUID) =
            when (this) {
                is HeroTrailerSource.Local -> copy(itemId = itemId)
                is HeroTrailerSource.Direct -> copy(itemId = itemId)
            }

        private data class Optional(
            val source: HeroTrailerSource?,
        )

        companion object {
            val START_DELAY = 5.seconds

            // How long an item stays focused before its trailer is looked for and got ready, part of START_DELAY
            val PREPARE_DELAY = 1.seconds
            val THEME_MUSIC_DELAY = 1.seconds
        }
    }

/**
 * Remux turns every trailer its add-ons give into a YouTube link, so a link to a video elsewhere arrives as
 * youtube.com/watch?v=https://... and is taken back out here
 */
internal fun String.unwrapTrailerUrl(): String =
    if (isYouTubeUrl()) {
        toUri().getQueryParameter("v")?.takeIf { it.isWebUrl() } ?: this
    } else {
        this
    }

/**
 * Whether this links to a video to play rather than to a page, which is all that is known without loading it
 */
internal fun String.isVideoUrl() = isWebUrl() && !isYouTubeUrl() && !isAudioUrl()

/**
 * Whether this links to an audio file, such as theme music an add-on put among the trailers since Remux has nowhere
 * else for it
 */
internal fun String.isAudioUrl(): Boolean {
    if (!isWebUrl()) return false
    val path = toUri().path?.lowercase() ?: return false
    return AUDIO_EXTENSIONS.any { path.endsWith(it) }
}

private val AUDIO_EXTENSIONS = listOf(".m4a", ".mp3", ".aac", ".opus", ".ogg", ".oga", ".flac", ".wav")

private fun String.isWebUrl() = startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

private fun String.isYouTubeUrl(): Boolean {
    val host = toUri().host?.lowercase() ?: return false
    return host.endsWith("youtube.com") || host.endsWith("youtu.be") || host.endsWith("youtube-nocookie.com")
}
