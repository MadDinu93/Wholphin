package com.github.damontecres.wholphin.services

import androidx.datastore.core.DataStore
import com.github.damontecres.wholphin.preferences.AppPreferences
import com.github.damontecres.wholphin.services.hilt.StandardOkHttpClient
import com.github.damontecres.wholphin.util.WholphinDispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds theme music from add-ons for titles the server has none of
 *
 * First from the theme music add-on set in the settings: any add-on works that answers
 * `GET {address}/themes/{type}/{id}.json`, with `movie` or `series` as the type and `tmdb:603` or an IMDb id such as
 * `tt0133093` as the id, with `{"themes": [{"url": "https://..."}]}`, each url a link to an audio file. Thorphin Trailers
 * is one, its themes come from ThemerrDB.
 *
 * Otherwise from links to audio files among the item's remote trailers, the same way trailers come from add-ons, since
 * Remux has nowhere else for them. Thorphin Trailers puts its theme music there too.
 */
@Singleton
class ThemeMusicAddonService
    @Inject
    constructor(
        @param:StandardOkHttpClient private val okHttpClient: OkHttpClient,
        private val api: ApiClient,
        private val preferences: DataStore<AppPreferences>,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        // Keyed by the request's address, an add-on's answer changes rarely
        private val cache = ConcurrentHashMap<String, List<String>>()

        /**
         * Links to the theme music of the movie, series or collection [itemId], empty if there is no add-on or it has none
         */
        suspend fun getThemeUrls(itemId: UUID): List<String> {
            val address =
                preferences.data
                    .first()
                    .interfacePreferences.themeMusicAddonUrl
                    .trim()
                    .removeSuffix("/manifest.json")
                    .trimEnd('/')
            return try {
                // Rows do not fetch the provider ids or the trailers, so get the full item
                val item = api.userLibraryApi.getItem(itemId = itemId).content
                address
                    .takeIf { it.isNotBlank() }
                    ?.let { fromAddon(it, item) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: fromRemoteTrailers(item)
            } catch (ex: Exception) {
                Timber.w(ex, "Could not get theme music for %s from add-ons", itemId)
                emptyList()
            }
        }

        private suspend fun fromAddon(
            address: String,
            item: BaseItemDto,
        ): List<String> {
            val type =
                when (item.type) {
                    BaseItemKind.MOVIE -> "movie"
                    BaseItemKind.SERIES -> "series"
                    else -> return emptyList()
                }
            val providerIds = item.providerIds.orEmpty().mapKeys { it.key.lowercase() }
            val id =
                providerIds["tmdb"]?.takeIf { it.isNotBlank() }?.let { "tmdb:$it" }
                    ?: providerIds["imdb"]?.takeIf { it.isNotBlank() }
                    ?: return emptyList()
            val url = "$address/themes/$type/$id.json"
            return try {
                cache[url] ?: fetch(url).also { cache[url] = it }
            } catch (ex: Exception) {
                // The remote trailers may still have it
                Timber.w(ex, "Could not get theme music for %s from the add-on", item.id)
                emptyList()
            }
        }

        private fun fromRemoteTrailers(item: BaseItemDto): List<String> =
            item.remoteTrailers
                ?.mapNotNull { it.url?.unwrapTrailerUrl()?.takeIf { url -> url.isAudioUrl() } }
                .orEmpty()

        private suspend fun fetch(url: String): List<String> =
            withContext(WholphinDispatchers.IO) {
                okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    when {
                        response.code == 404 -> {
                            emptyList()
                        }

                        !response.isSuccessful -> {
                            // Not kept, so it is asked again next time
                            throw IllegalStateException("HTTP ${response.code}")
                        }

                        else -> {
                            json
                                .decodeFromString<ThemesResponse>(response.body.string())
                                .themes
                                .map { it.url }
                                .filter { it.startsWith("https://") || it.startsWith("http://") }
                        }
                    }
                }
            }

        @Serializable
        private data class ThemesResponse(
            val themes: List<Theme> = emptyList(),
        )

        @Serializable
        private data class Theme(
            val url: String,
        )
    }
