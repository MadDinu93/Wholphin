package com.github.damontecres.wholphin.ui.playback

import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStreamType
import org.jellyfin.sdk.model.api.PlaybackInfoDto
import timber.log.Timber
import java.util.UUID

/**
 * Some servers (eg Remux) only list subtitle streams, including external ones provided by addons, in the
 * PlaybackInfo response and not in the item's media sources. If [source] has no subtitle streams, fetch
 * PlaybackInfo for it and use the streams from that response instead.
 */
internal suspend fun PlaybackViewModel.withPlaybackInfoStreams(
    itemId: UUID,
    source: MediaSourceInfo,
): MediaSourceInfo {
    if (source.mediaStreams.orEmpty().any { it.type == MediaStreamType.SUBTITLE }) {
        return source
    }
    return try {
        val response =
            api.mediaInfoApi
                .getPostedPlaybackInfo(
                    itemId,
                    PlaybackInfoDto(
                        mediaSourceId = source.id,
                        enableDirectPlay = true,
                        enableDirectStream = true,
                        enableTranscoding = false,
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
            source.copy(
                mediaStreams = streams,
                defaultAudioStreamIndex = match.defaultAudioStreamIndex ?: source.defaultAudioStreamIndex,
                defaultSubtitleStreamIndex = match.defaultSubtitleStreamIndex ?: source.defaultSubtitleStreamIndex,
            )
        }
    } catch (ex: Exception) {
        Timber.w(ex, "Could not fetch PlaybackInfo streams for %s", itemId)
        source
    }
}
