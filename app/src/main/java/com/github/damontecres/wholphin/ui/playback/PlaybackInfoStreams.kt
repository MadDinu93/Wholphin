package com.github.damontecres.wholphin.ui.playback

import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import org.jellyfin.sdk.model.api.VideoRange
import org.jellyfin.sdk.model.api.VideoRangeType
import timber.log.Timber
import java.util.UUID

internal fun MediaSourceInfo.simpleVideoStream(): SimpleVideoStream? =
    mediaStreams
        ?.firstOrNull { it.type == MediaStreamType.VIDEO }
        ?.let {
            val isHdr =
                it.videoRange == VideoRange.HDR ||
                    (it.videoRangeType != VideoRangeType.SDR && it.videoRangeType != VideoRangeType.UNKNOWN)
            // Often times 4k movies have a wider aspect ratio so the height is lower even though the width is still 3840
            val is4k = (it.width ?: 0) > 2560 || (it.height ?: 0) > 1440
            SimpleVideoStream(it.index, isHdr, is4k)
        }

/**
 * Some servers (eg Remux) only list subtitle streams, including external ones provided by addons, in the
 * PlaybackInfo response and not in the item's media sources. If [source] has no subtitle streams, fetch
 * PlaybackInfo for it and use the streams from that response instead.
 *
 * The request carries the same device profile and bitrate limit as the playback request, because the server
 * may pick the release and number the tracks based on them. [remapToPlayedSource] then checks the chosen
 * tracks against the source actually played.
 */
internal suspend fun PlaybackViewModel.withPlaybackInfoStreams(
    itemId: UUID,
    source: MediaSourceInfo,
): MediaSourceInfo {
    if (source.mediaStreams.orEmpty().any { it.type == MediaStreamType.SUBTITLE }) {
        return source
    }
    return try {
        val video = source.simpleVideoStream()
        val backend = choosePlayerBackend(video?.hdr == true, video?.is4k == true)
        val response =
            api.mediaInfoApi
                .getPostedPlaybackInfo(
                    itemId,
                    PlaybackInfoDto(
                        mediaSourceId = source.id,
                        deviceProfile = deviceProfileFor(backend),
                        maxStreamingBitrate = maxStreamingBitrate,
                        enableDirectPlay = !forceTranscoding,
                        enableDirectStream = !forceTranscoding,
                        enableTranscoding = true,
                        autoOpenLiveStream = false,
                    ),
                ).content
        val match =
            response.mediaSources.firstOrNull { it.id == source.id }
                ?: response.mediaSources.singleOrNull()
        val streams = match?.mediaStreams
        if (match == null || streams.isNullOrEmpty()) {
            source
        } else {
            Timber.i(
                "Using %s streams from PlaybackInfo for source %s (item had %s)",
                streams.size,
                source.id,
                source.mediaStreams?.size,
            )
            source
                .copy(
                    mediaStreams = streams,
                    defaultAudioStreamIndex = match.defaultAudioStreamIndex ?: source.defaultAudioStreamIndex,
                    defaultSubtitleStreamIndex = match.defaultSubtitleStreamIndex ?: source.defaultSubtitleStreamIndex,
                ).also { playbackInfoStreamsSource = it }
        }
    } catch (ex: Exception) {
        Timber.w(ex, "Could not fetch PlaybackInfo streams for %s", itemId)
        source
    }
}

/**
 * The server may still answer the playback request with another release than the separate PlaybackInfo request
 * the track menus came from, and the same index can then be another track (eg "English" playing Ukrainian).
 * Finds the chosen audio and subtitle tracks in [played] and rebuilds the menus from it.
 *
 * Returns the indexes to use with [played]; a track that [played] does not have is dropped (no subtitle, the
 * default audio) rather than playing another language.
 */
internal suspend fun PlaybackViewModel.remapToPlayedSource(
    played: MediaSourceInfo,
    audioIndex: Int?,
    subtitleIndex: Int?,
): Pair<Int?, Int?> {
    val listed = playbackInfoStreamsSource ?: return audioIndex to subtitleIndex
    playbackInfoStreamsSource = null
    // A missing audio track falls back to the source's default
    val audio = sameTrack(listed, played, audioIndex, MediaStreamType.AUDIO)
    val subtitle = sameTrack(listed, played, subtitleIndex, MediaStreamType.SUBTITLE)
    if (audio != audioIndex || subtitle != subtitleIndex) {
        Timber.w(
            "Played source %s numbers tracks differently: audio %s -> %s, subtitle %s -> %s",
            played.id,
            audioIndex,
            audio,
            subtitleIndex,
            subtitle,
        )
    }
    updateCurrentMedia {
        it.copy(
            sourceId = played.id,
            audioStreams = getAudioStreams(played),
            subtitleStreams = getSubtitleStreams(played),
        )
    }
    return audio to subtitle
}

private fun MediaStream.trackKey() = listOf(language, codec, title, isExternal, isForced, isHearingImpaired)

/**
 * The index in [played] of the track [index] points to in [listed]: the same index when the track there is the
 * same, else the track at the same position among the equal ones. Null when [played] has no such track.
 */
private fun sameTrack(
    listed: MediaSourceInfo,
    played: MediaSourceInfo,
    index: Int?,
    type: MediaStreamType,
): Int? {
    if (index == null || index < 0) return index
    val ofType = { source: MediaSourceInfo -> source.mediaStreams.orEmpty().filter { it.type == type } }
    val chosen = ofType(listed).firstOrNull { it.index == index } ?: return index
    val key = chosen.trackKey()
    val candidates = ofType(played).filter { it.trackKey() == key }
    if (candidates.any { it.index == index }) return index
    val position = ofType(listed).filter { it.trackKey() == key }.indexOfFirst { it.index == index }
    return (candidates.getOrNull(position) ?: candidates.firstOrNull())?.index
}
