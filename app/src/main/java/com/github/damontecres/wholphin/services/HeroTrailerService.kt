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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
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

    /** A YouTube trailer, played with the embedded YouTube player */
    data class YouTube(
        override val itemId: UUID,
        val videoId: String,
    ) : HeroTrailerSource
}

/**
 * Picks a trailer for the item focused in the hero once it has stayed focused for a while, Netflix style
 */
@Singleton
class HeroTrailerService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:AuthOkHttpClient private val authOkHttpClient: OkHttpClient,
        private val api: ApiClient,
        private val trailerService: TrailerService,
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

        /**
         * The hero now shows [item], or nothing if null. Stops any trailer and starts the delay for the new one.
         *
         * With an [owner], such as a page, clearing it with a null [item] only happens if no one else has asked for a
         * trailer since. Pages starting and closing during navigation then do not stop each other's trailers.
         */
        fun focus(
            item: BaseItem?,
            owner: Any? = null,
            inCard: Boolean = false,
        ) {
            if (item == null && owner != null && owner !== this.owner) return
            this.owner = owner
            job?.cancel()
            _current.value = null
            _playing.value = false
            _inCard.value = inCard
            if (item == null) return
            job =
                scope.launch {
                    delay(START_DELAY)
                    val source = resolve(item)
                    Timber.v("Hero trailer for %s: %s", item.id, source)
                    _current.value = source
                }
        }

        /** The trailer for [itemId] started playing */
        fun onPlaying(itemId: UUID) {
            if (_current.value?.itemId == itemId) {
                _playing.value = true
            }
        }

        /** The trailer for [itemId] ended or failed, go back to the image */
        fun finished(itemId: UUID) {
            if (_current.value?.itemId == itemId) {
                _current.value = null
                _playing.value = false
            }
        }

        @OptIn(UnstableApi::class)
        fun createPlayer(): ExoPlayer =
            ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(authOkHttpClient)))
                .build()

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
                        trailerService
                            .getRemoteTrailers(full)
                            .firstNotNullOfOrNull { (it as? RemoteTrailer)?.url?.youTubeVideoId() }
                            ?.let { HeroTrailerSource.YouTube(itemId = item.id, videoId = it) }
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
                is HeroTrailerSource.YouTube -> copy(itemId = itemId)
            }

        private data class Optional(
            val source: HeroTrailerSource?,
        )

        companion object {
            val START_DELAY = 5.seconds
        }
    }

/**
 * The video id of a youtube.com/watch?v=, youtu.be/ or youtube.com/embed/ URL
 */
internal fun String.youTubeVideoId(): String? {
    val uri = this.toUri()
    val host = uri.host?.removePrefix("www.")?.removePrefix("m.") ?: return null
    val id =
        when (host) {
            "youtube.com", "youtube-nocookie.com" -> {
                uri.getQueryParameter("v")
                    ?: uri.pathSegments
                        .takeIf { it.size >= 2 && (it[0] == "embed" || it[0] == "shorts") }
                        ?.get(1)
            }

            "youtu.be" -> {
                uri.pathSegments.firstOrNull()
            }

            else -> {
                null
            }
        }
    return id?.takeIf { YOUTUBE_ID.matches(it) }
}

private val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")
