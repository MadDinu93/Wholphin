package com.github.damontecres.wholphin.services

import android.content.Context
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.preferences.ThemeSongVolume
import com.github.damontecres.wholphin.services.hilt.AuthOkHttpClient
import com.github.damontecres.wholphin.services.hilt.IoCoroutineScope
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.util.WholphinDispatchers
import com.github.damontecres.wholphin.util.profile.Codec
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.universalAudioApi
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple service to play theme song music: from the Jellyfin server, or else from the theme music add-on
 */
@OptIn(UnstableApi::class)
@Singleton
class ThemeSongPlayer
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:AuthOkHttpClient private val authOkHttpClient: OkHttpClient,
        @param:StandardOkHttpClient private val standardOkHttpClient: OkHttpClient,
        private val api: ApiClient,
        private val themeMusicAddonService: ThemeMusicAddonService,
        @param:IoCoroutineScope private val scope: CoroutineScope,
        private val preferences: DataStore<AppPreferences>,
    ) {
        private val mutex = Mutex()
        private val state = MutableStateFlow(ThemeSongState())

        private val player: ExoPlayer by lazy { ExoPlayer.Builder(context).build() }

        // Only music on the server gets the user's credentials, not links to elsewhere from the add-on
        private val serverSourceFactory by lazy { DefaultMediaSourceFactory(OkHttpDataSource.Factory(authOkHttpClient)) }
        private val addonSourceFactory by lazy { DefaultMediaSourceFactory(OkHttpDataSource.Factory(standardOkHttpClient)) }

        /**
         * @param owner who asked for it, such as a page, so that [stop] with the same owner stops only their music
         */
        fun playThemeFor(
            itemId: UUID,
            owner: Any? = null,
        ) = load(itemId, owner, play = true)

        /**
         * Gets the theme music of [itemId] ready without playing it, such as while a trailer plays, so that
         * [playThemeFor] with the same item and [owner] starts it at once
         */
        fun prepareThemeFor(
            itemId: UUID,
            owner: Any,
        ) = load(itemId, owner, play = false)

        private fun load(
            itemId: UUID,
            owner: Any?,
            play: Boolean,
        ) {
            scope.launch {
                mutex.withLock {
                    state.value.job?.join()
                    val current = state.value
                    if (play && current.prepared && current.itemId == itemId && current.owner === owner) {
                        withContext(WholphinDispatchers.Main) { player.play() }
                        state.update { it.copy(prepared = false) }
                        return@withLock
                    }
                    val volumeLevel =
                        when (
                            preferences.data
                                .first()
                                .interfacePreferences.playThemeSongs
                        ) {
                            ThemeSongVolume.UNRECOGNIZED,
                            ThemeSongVolume.DISABLED,
                            -> return@withLock

                            ThemeSongVolume.LOWEST -> .05f

                            ThemeSongVolume.LOW -> .1f

                            ThemeSongVolume.MEDIUM -> .25f

                            ThemeSongVolume.HIGH -> .5f

                            ThemeSongVolume.HIGHEST -> .75f
                        }
                    val job =
                        scope.launchIO {
                            val themeSongs by api.libraryApi.getThemeSongs(itemId)
                            val serverUrl =
                                themeSongs.items.randomOrNull()?.let { theme ->
                                    api.universalAudioApi.getUniversalAudioStreamUrl(
                                        theme.id,
                                        container =
                                            listOf(
                                                Codec.Audio.OPUS,
                                                Codec.Audio.MP3,
                                                Codec.Audio.AAC,
                                                Codec.Audio.FLAC,
                                            ),
                                    )
                                }
                            val url = serverUrl ?: themeMusicAddonService.getThemeUrls(itemId).randomOrNull()
                            if (url == null) {
                                Timber.v("No theme song for %s", itemId)
                                return@launchIO
                            }
                            val sourceFactory = if (serverUrl != null) serverSourceFactory else addonSourceFactory
                            withContext(WholphinDispatchers.Main) {
                                player.apply {
                                    stop()
                                    volume = volumeLevel
                                    setMediaSource(sourceFactory.createMediaSource(MediaItem.fromUri(url)))
                                    prepare()
                                    playWhenReady = play
                                }
                            }
                        }
                    state.update {
                        it.copy(
                            itemId = itemId,
                            owner = owner,
                            job = job,
                            prepared = !play,
                        )
                    }
                }
            }
        }

        /**
         * @param owner if given, only stops the music if it is what they asked [playThemeFor]
         */
        fun stop(owner: Any? = null) {
            scope.launch {
                mutex.withLock {
                    if (owner != null && state.value.owner !== owner) return@withLock
                    state.value.job?.cancelAndJoin()
                    withContext(WholphinDispatchers.Main) {
                        Timber.v("Stopping theme song")
                        player.stop()
                    }
                    state.update {
                        it.copy(
                            itemId = null,
                            owner = null,
                            job = null,
                            prepared = false,
                        )
                    }
                }
            }
        }
    }

data class ThemeSongState(
    val itemId: UUID? = null,
    val owner: Any? = null,
    val job: Job? = null,
    // Loaded by prepareThemeFor and not playing yet
    val prepared: Boolean = false,
)
