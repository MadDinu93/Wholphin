package com.github.damontecres.wholphin.services

import android.content.Context
import com.github.damontecres.wholphin.BuildConfig
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.HomeRowConfig
import com.github.damontecres.wholphin.data.model.HomeRowViewOptions
import com.github.damontecres.wholphin.ui.toServerString
import com.github.damontecres.wholphin.ui.util.StringProvider
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.requestSerializersModule
import com.github.damontecres.wholphin.services.hilt.DefaultCoroutineScope
import com.github.damontecres.wholphin.util.WholphinDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

/**
 * Keeps the last home page rows on disk, so the home page can be shown right away at startup while the
 * rows are fetched again
 *
 * Only the first [ROWS_WITH_ITEMS] rows keep their first [ITEMS_PER_ROW] items, the rest are only
 * placeholders. A cache from
 * another app version is ignored, since [com.github.damontecres.wholphin.ui.util.ResStringProvider]s hold
 * resource IDs which can change between builds.
 */
@Singleton
class HomeRowsCache
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:DefaultCoroutineScope private val coroutineScope: CoroutineScope,
    ) {
        @Volatile
        private var preloaded: Preloaded? = null

        private fun file(
            serverId: UUID,
            userId: UUID,
        ) = File(File(context.cacheDir, DIR), "${serverId.toServerString()}_${userId.toServerString()}.json")

        /**
         * Start reading the cache at app start, while signing in, so it is ready when the home page is
         */
        fun preload(
            serverId: UUID,
            userId: UUID,
        ) {
            preloaded =
                Preloaded(serverId, userId, coroutineScope.async(WholphinDispatchers.IO) { read(serverId, userId) })
        }

        suspend fun load(
            serverId: UUID,
            userId: UUID,
        ): List<HomeRowLoadingState>? {
            val preloaded = preloaded
            this.preloaded = null
            return if (preloaded != null && preloaded.serverId == serverId && preloaded.userId == userId) {
                preloaded.rows.await()
            } else {
                preloaded?.rows?.cancel()
                withContext(WholphinDispatchers.IO) { read(serverId, userId) }
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private fun read(
            serverId: UUID,
            userId: UUID,
        ): List<HomeRowLoadingState>? =
            try {
                val file = file(serverId, userId)
                if (file.exists()) {
                    val start = TimeSource.Monotonic.markNow()
                    val cached = file.inputStream().use { json.decodeFromStream<CachedHome>(it) }
                    Timber.v("Read home cache of %s bytes in %s", file.length(), start.elapsedNow())
                    if (cached.versionCode == BuildConfig.VERSION_CODE) {
                        cached.rows.map { it.toState() }
                    } else {
                        Timber.d("Ignoring home cache from version %s", cached.versionCode)
                        null
                    }
                } else {
                    null
                }
            } catch (ex: Exception) {
                Timber.w(ex, "Could not read home cache")
                null
            }

        @OptIn(ExperimentalSerializationApi::class)
        fun save(
            serverId: UUID,
            userId: UUID,
            rows: List<HomeRowLoadingState>,
        ) {
            try {
                val cached =
                    CachedHome(
                        versionCode = BuildConfig.VERSION_CODE,
                        rows = rows.mapIndexed { index, row -> CachedHomeRow.from(row, index < ROWS_WITH_ITEMS) },
                    )
                val file = file(serverId, userId)
                file.parentFile?.mkdirs()
                // Written next to it first, so a half written file is never read
                val temp = File(file.parentFile, file.name + ".tmp")
                temp.outputStream().use { json.encodeToStream(cached, it) }
                temp.renameTo(file)
            } catch (ex: Exception) {
                Timber.w(ex, "Could not write home cache")
            }
        }

        companion object {
            private const val DIR = "home_rows"

            /**
             * Enough to fill the screen and a bit of scrolling, it has to be read quickly at startup
             */
            private const val ROWS_WITH_ITEMS = 4
            const val ITEMS_PER_ROW = 12

            private val json =
                Json {
                    classDiscriminator = "_type"
                    serializersModule = requestSerializersModule
                    ignoreUnknownKeys = true
                }
        }
    }

private class Preloaded(
    val serverId: UUID,
    val userId: UUID,
    val rows: Deferred<List<HomeRowLoadingState>?>,
)

@Serializable
private data class CachedHome(
    val versionCode: Int,
    val rows: List<CachedHomeRow>,
)

/**
 * A home row, [items] is null when only its title is kept
 */
@Serializable
private data class CachedHomeRow(
    val title: StringProvider,
    val items: List<BaseItem>?,
    val viewOptions: HomeRowViewOptions,
    val rowType: HomeRowConfig?,
    val showViewMore: Boolean,
) {
    fun toState(): HomeRowLoadingState =
        if (items != null) {
            HomeRowLoadingState.Success(title, items, viewOptions, rowType, showViewMore)
        } else {
            HomeRowLoadingState.Pending(title)
        }

    companion object {
        fun from(
            row: HomeRowLoadingState,
            withItems: Boolean,
        ): CachedHomeRow =
            when (row) {
                is HomeRowLoadingState.Success -> {
                    CachedHomeRow(
                        title = row.title,
                        // Empty rows are hidden, so they are cheap to keep
                        items =
                            if (withItems || row.items.isEmpty()) {
                                row.items
                                    .filterNotNull()
                                    .take(HomeRowsCache.ITEMS_PER_ROW)
                                    // The cast & crew are most of the size, cards don't show them
                                    .map { it.copy(data = it.data.copy(people = null)) }
                            } else {
                                null
                            },
                        viewOptions = row.viewOptions,
                        rowType = row.rowType,
                        showViewMore = row.showViewMore,
                    )
                }

                else -> {
                    CachedHomeRow(row.title, null, HomeRowViewOptions(), null, false)
                }
            }
    }
}
