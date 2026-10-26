package com.github.damontecres.wholphin.ui.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Chapter
import com.github.damontecres.wholphin.services.getFallbackSubtitleLanguage
import com.github.damontecres.wholphin.services.getPreferredLanguage
import com.github.damontecres.wholphin.services.isSameLanguage
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.TrickplayInfo
import org.jellyfin.sdk.model.extensions.ticks

/**
 * Metadata about the currently playing media
 *
 * @see CurrentPlayback
 */
data class CurrentMediaInfo(
    val sourceId: String?,
    val videoStream: SimpleVideoStream?,
    val audioStreams: List<SimpleMediaStream>,
    val subtitleStreams: List<SimpleMediaStream>,
    val chapters: List<Chapter>,
    val trickPlayInfo: TrickplayInfo?,
) {
    companion object {
        val EMPTY = CurrentMediaInfo(null, null, listOf(), listOf(), listOf(), null)
    }
}

/**
 * Gets a sorted list of subtitle streams in a [MediaSourceInfo]
 *
 * The preferred language comes first, the subtitles inside the file before the external ones, then the fallback language
 * the same way, then the rest. Within each, the server's order is kept since it may rank them by how well they match.
 * When only the preferred subtitles are shown, the rest are marked as hidden.
 */
internal fun PlaybackViewModel.getSubtitleStreams(mediaSource: MediaSourceInfo): List<SimpleMediaStream> {
    val prefs = preferences
    val preferredLanguage =
        getPreferredLanguage(MediaStreamType.SUBTITLE, prefs, serverRepository.currentUserDto?.configuration)
    val fallbackLanguage = getFallbackSubtitleLanguage(prefs)
    val onlyPreferred =
        prefs.appPreferences.playbackPreferences.onlyPreferredSubtitles &&
            (preferredLanguage != null || fallbackLanguage != null)

    fun MediaStream.rank(): Int =
        when {
            language.isSameLanguage(preferredLanguage) -> if (isExternal) 1 else 0
            language.isSameLanguage(fallbackLanguage) -> if (isExternal) 3 else 2
            else -> 4
        }
    return mediaSource.mediaStreams
        ?.filter { it.type == MediaStreamType.SUBTITLE }
        // sortedBy is stable, so the server's order is kept within each rank
        ?.sortedBy { it.rank() }
        ?.map {
            // TODO should use a string provider instead
            SimpleMediaStream
                .from(context.resources, it, true)
                .copy(hidden = onlyPreferred && it.rank() == 4)
        }.orEmpty()
}

/**
 * Gets a list of audio streams in a [MediaSourceInfo]
 */
internal fun PlaybackViewModel.getAudioStreams(mediaSource: MediaSourceInfo): List<SimpleMediaStream> =
    mediaSource.mediaStreams
        ?.filter { it.type == MediaStreamType.AUDIO }
        ?.map {
            SimpleMediaStream.from(context.resources, it, true)
        }
//                        ?.sortedWith(compareBy<AudioStream> { it.language }.thenByDescending { it.channels })
        .orEmpty()

fun BaseItem.toMediaMetadata(imageUrl: String?): MediaMetadata =
    MediaMetadata
        .Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setReleaseYear(data.productionYear)
        .setDescription(data.overview)
        .setArtworkUri(imageUrl?.toUri())
        .setDurationMs(data.runTimeTicks?.ticks?.inWholeMilliseconds)
        .setMediaType(
            when (type) {
                BaseItemKind.MOVIE -> MediaMetadata.MEDIA_TYPE_MOVIE
                BaseItemKind.EPISODE -> MediaMetadata.MEDIA_TYPE_TV_SHOW
                BaseItemKind.VIDEO -> MediaMetadata.MEDIA_TYPE_VIDEO
                BaseItemKind.TV_CHANNEL, BaseItemKind.CHANNEL, BaseItemKind.LIVE_TV_CHANNEL -> MediaMetadata.MEDIA_TYPE_TV_CHANNEL
                else -> MediaMetadata.MEDIA_TYPE_VIDEO
            },
        ).build()
