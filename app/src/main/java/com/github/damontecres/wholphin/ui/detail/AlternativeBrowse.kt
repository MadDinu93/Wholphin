package com.github.damontecres.wholphin.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import android.text.format.DateFormat
import androidx.compose.runtime.LaunchedEffect
import com.github.damontecres.wholphin.ui.ifElse
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import com.github.damontecres.wholphin.util.localizeServerName
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalContext
import com.github.damontecres.wholphin.data.filter.DefaultFilterOptions
import com.github.damontecres.wholphin.data.filter.DefaultTvFilterOptions
import com.github.damontecres.wholphin.data.filter.FilterValueOption
import com.github.damontecres.wholphin.data.filter.ItemFilterBy
import com.github.damontecres.wholphin.data.model.GetItemsFilter
import com.github.damontecres.wholphin.services.FilterOptionCache
import com.github.damontecres.wholphin.ui.components.FilterByButton
import java.util.Date
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.HeroTrailerSource
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.ui.main.settings.Library
import com.github.damontecres.wholphin.ui.nav.ZoomedTrailerPlayer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jellyfin.sdk.model.api.CollectionType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.SlimItemFields
import com.github.damontecres.wholphin.ui.components.QuickDetails
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.main.streamBadges
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.LocalHomeScreenStyle
import com.github.damontecres.wholphin.ui.theme.isMinimalist
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.ApiRequestPager
import com.github.damontecres.wholphin.util.GetItemsRequestHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import javax.inject.Inject

/** Which titles the Alternative UI browse page lists */
enum class BrowseKind { MOVIES, SERIES }

/** The titles of a library, or of every library of the kind when [library] is null */
data class BrowseCatalogue(
    val library: Library?,
)

private val BrowseKind.itemKind get() = if (this == BrowseKind.MOVIES) BaseItemKind.MOVIE else BaseItemKind.SERIES

private val BrowseKind.collectionType get() = if (this == BrowseKind.MOVIES) CollectionType.MOVIES else CollectionType.TVSHOWS

data class BrowseState(
    val catalogues: List<BrowseCatalogue> = listOf(BrowseCatalogue(null)),
    val selected: Int = 0,
    val pager: ApiRequestPager<GetItemsRequest>? = null,
    val loading: Boolean = true,
    val filter: GetItemsFilter = GetItemsFilter(),
)

@HiltViewModel
class AlternativeBrowseViewModel
    @Inject
    constructor(
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
        private val imageUrlService: ImageUrlService,
        private val navDrawerService: NavDrawerService,
        private val filterOptionCache: FilterOptionCache,
        val heroTrailerService: HeroTrailerService,
        val navigationManager: NavigationManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BrowseState())
        val state: StateFlow<BrowseState> = _state

        private var kind: BrowseKind? = null
        private var loadJob: Job? = null

        /** Starts with "All", the libraries of the kind are added as soon as the server's libraries are known */
        fun start(kind: BrowseKind) {
            if (this.kind == kind) return
            this.kind = kind
            load(0, immediately = true)
            viewModelScope.launchIO {
                navDrawerService.state
                    .map { state -> state.allLibraries.filter { it.collectionType == kind.collectionType && !it.isRecordingFolder } }
                    .distinctUntilChanged()
                    .collect { libraries ->
                        _state.update { it.copy(catalogues = listOf(BrowseCatalogue(null)) + libraries.map(::BrowseCatalogue)) }
                    }
            }
        }

        /** The filters apply to every catalogue */
        fun setFilter(filter: GetItemsFilter) {
            _state.update { it.copy(filter = filter) }
            load(_state.value.selected, immediately = true)
        }

        suspend fun filterValues(option: ItemFilterBy<*>): List<FilterValueOption> =
            filterOptionCache.getFilterOptionValues(
                _state.value.catalogues
                    .getOrNull(_state.value.selected)
                    ?.library
                    ?.itemId,
                option,
            )

        /** Moving over the catalogues selects them, a short wait keeps fast scrolling from asking for each one */
        fun select(index: Int) {
            if (index == _state.value.selected && _state.value.pager != null) return
            load(index, immediately = false)
        }

        private fun load(
            index: Int,
            immediately: Boolean,
        ) {
            val catalogue = _state.value.catalogues.getOrNull(index) ?: return
            _state.update { it.copy(selected = index, loading = true) }
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (!immediately) delay(250)
                    try {
                        val pager =
                            ApiRequestPager(api, request(catalogue), GetItemsRequestHandler, viewModelScope).init()
                        _state.update { it.copy(pager = pager, loading = false) }
                    } catch (ex: kotlinx.coroutines.CancellationException) {
                        throw ex
                    } catch (ex: Exception) {
                        Timber.e(ex, "Could not load %s", catalogue.library?.name)
                        _state.update { it.copy(pager = null, loading = false) }
                    }
                }
        }

        private fun request(catalogue: BrowseCatalogue) =
            _state.value.filter.applyTo(
                GetItemsRequest(
                userId = serverRepository.currentUser?.id,
                parentId = catalogue.library?.itemId,
                includeItemTypes = listOf(checkNotNull(kind).itemKind),
                recursive = true,
                sortBy = listOf(ItemSortBy.SORT_NAME, ItemSortBy.PRODUCTION_YEAR),
                sortOrder = listOf(SortOrder.ASCENDING),
                fields = SlimItemFields + ItemFields.MEDIA_STREAMS,
                enableImageTypes = listOf(ImageType.PRIMARY, ImageType.BACKDROP, ImageType.THUMB),
                ),
                overwriteIncludeTypes = false,
            )

        fun posterUrl(
            item: BaseItem?,
            widthPx: Int,
        ): String? = imageUrlService.getItemImageUrl(item, ImageType.PRIMARY, fillWidth = widthPx)

        fun backdropUrl(
            item: BaseItem?,
            widthPx: Int,
        ): String? = imageUrlService.getItemImageUrl(item, ImageType.BACKDROP, fillWidth = widthPx)

        private val seriesGenres = java.util.concurrent.ConcurrentHashMap<java.util.UUID, List<String>>()

        /**
         * The genres of [item]. Episodes and seasons (the rows of Home like Continue watching) carry none, so they get
         * the ones of their series, asked for once per series.
         */
        suspend fun genres(item: BaseItem): List<String> {
            val own: List<String> = item.data.genres.orEmpty().ifEmpty { item.data.genreItems.orEmpty().mapNotNull { it.name } }
            val seriesId = item.data.seriesId
            if (own.isNotEmpty() || seriesId == null) return own
            seriesGenres[seriesId]?.let { return it }
            return try {
                val series = api.userLibraryApi.getItem(itemId = seriesId).content
                val found: List<String> = series.genres.orEmpty().ifEmpty { series.genreItems.orEmpty().mapNotNull { it.name } }
                seriesGenres[seriesId] = found
                found
            } catch (ex: kotlinx.coroutines.CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Timber.w(ex, "Could not load the genres of series %s", seriesId)
                emptyList()
            }
        }

        fun open(item: BaseItem) = navigationManager.navigateTo(item.destination())

        fun play(item: BaseItem) = navigationManager.navigateTo(Destination.Playback(item))
    }

/**
 * Movies and Series of the Alternative UI, see [AlternativeBrowseLayout]. Focusing a catalogue shows its titles.
 */
@Composable
fun AlternativeBrowsePage(
    kind: BrowseKind,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: AlternativeBrowseViewModel = hiltViewModel(key = kind.name),
) {
    LaunchedEffect(kind) { viewModel.start(kind) }
    val state by viewModel.state.collectAsState()
    val allTitle = stringResource(if (kind == BrowseKind.MOVIES) R.string.browse_all_movies else R.string.browse_all_series)
    val glyph = stringResource(if (kind == BrowseKind.MOVIES) R.string.fa_film else R.string.fa_tv)
    val context = LocalContext.current
    val entries =
        remember(state.catalogues, allTitle, glyph) {
            state.catalogues.map { catalogue ->
                // Only "All" has an icon, the libraries are told apart by their initials when the list is narrow
                BrowseEntry(localizeServerName(context, catalogue.library?.name ?: allTitle), glyph.takeIf { catalogue.library == null })
            }
        }
    AlternativeBrowseLayout(
        heading = stringResource(if (kind == BrowseKind.MOVIES) R.string.movies_title else R.string.tv_shows_title),
        entries = entries,
        selected = state.selected,
        onSelect = viewModel::select,
        filter =
            BrowseFilter(
                options = if (kind == BrowseKind.MOVIES) DefaultFilterOptions else DefaultTvFilterOptions,
                current = state.filter,
                onChange = viewModel::setFilter,
                getValues = viewModel::filterValues,
            ),
        items = state.pager.orEmpty(),
        loading = state.loading,
        images = viewModel,
        onOpen = { _, item -> viewModel.open(item) },
        onPlay = viewModel::play,
        preferences = preferences,
        modifier = modifier,
    )
}

/** The filters above the titles, only given by the pages of Movies and Series */
data class BrowseFilter(
    val options: List<ItemFilterBy<*>>,
    val current: GetItemsFilter,
    val onChange: (GetItemsFilter) -> Unit,
    val getValues: suspend (ItemFilterBy<*>) -> List<FilterValueOption>,
)

/** A catalogue of the second column */
data class BrowseEntry(
    val title: String,
    val glyph: String?,
)

/**
 * The layout shared by Movies, Series and Home of the Alternative UI: the catalogues on the left, a three column grid
 * of posters in the middle and the details of the focused title on the right.
 */
@Composable
fun AlternativeBrowseLayout(
    heading: String,
    entries: List<BrowseEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    items: List<BaseItem?>,
    loading: Boolean,
    images: AlternativeBrowseViewModel,
    onOpen: (Int, BaseItem) -> Unit,
    onPlay: (BaseItem) -> Unit,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    filter: BrowseFilter? = null,
    showType: Boolean = false,
) {
    // Thorphin: the Minimalist design has its own layout for the same data
    if (LocalHomeScreenStyle.current.isMinimalist) {
        MinimalistBrowseLayout(
            heading = heading,
            entries = entries,
            selected = selected,
            onSelect = onSelect,
            items = items,
            loading = loading,
            images = images,
            onOpen = onOpen,
            onPlay = onPlay,
            preferences = preferences,
            modifier = modifier,
            filter = filter,
            showType = showType,
        )
        return
    }
    var focusedItem by remember { mutableStateOf<BaseItem?>(null) }
    val gridFocus = remember { FocusRequester() }
    var focusedOnce by remember { mutableStateOf(false) }
    // The first time titles are there the page starts on the posters, not on the catalogues
    LaunchedEffect(items.isNotEmpty()) {
        if (items.isNotEmpty() && !focusedOnce) {
            focusedOnce = true
            delay(100)
            gridFocus.tryRequestFocus()
        }
    }
    // Another catalogue starts with its first title
    LaunchedEffect(selected) { focusedItem = null }
    val shown = focusedItem ?: items.firstOrNull()
    // The trailer plays in the details instead of the picture, so it is asked for as "in card", which keeps the
    // backdrop of the whole app out of it
    val interfacePreferences = preferences.appPreferences.interfacePreferences
    val trailerService = images.heroTrailerService
    val trailersWanted = !interfacePreferences.disableHeroTrailer || interfacePreferences.cardTrailers
    val trailerOwner = remember { Any() }
    LaunchedEffect(shown?.id) {
        trailerService.focus(
            shown,
            trailerOwner,
            inCard = true,
            trailers = trailersWanted,
            themeMusic = interfacePreferences.cardThemeMusic,
        )
    }
    DisposableEffect(Unit) { onDispose { trailerService.focus(null, trailerOwner) } }
    val trailer by trailerService.current.collectAsStateWithLifecycle()
    val density = LocalConfiguration.current.densityDpi / 160f

    // The catalogues are only wide while they have the focus, once one is chosen they shrink to their icons and the
    // titles and details get the room
    var cataloguesFocused by remember { mutableStateOf(false) }
    var postersFocused by remember { mutableStateOf(false) }
    var detailsFocused by remember { mutableStateOf(false) }
    val contentFocused = postersFocused || detailsFocused
    val cataloguesWidth = animateDpAsState(if (cataloguesFocused) 215.dp else 100.dp, tween(250), label = "catalogues_width")
    val collapsed = !cataloguesFocused
    // Going back to the catalogues always lands on the one that is shown, not on the one next to the poster
    val selectedCatalogue = remember { FocusRequester() }
    BackHandler(enabled = contentFocused) { selectedCatalogue.tryRequestFocus() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxSize().padding(bottom = 12.dp),
    ) {
        // Catalogues
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    // Read while measuring, so only this panel is laid out again as it grows and shrinks
                    .layout { measurable, constraints ->
                        val width = cataloguesWidth.value.roundToPx()
                        val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }.fillMaxHeight()
                    .background(LiquidColors.Rail, RoundedCornerShape(12.dp))
                    .onFocusChanged { cataloguesFocused = it.hasFocus }
                    .focusGroup()
                    .focusProperties { onEnter = { selectedCatalogue.tryRequestFocus() } }
                    .padding(10.dp),
        ) {
            if (!collapsed) {
                Text(
                    text = heading,
                    color = LiquidColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            } else {
                // Keeps the rows where they are
                Spacer(Modifier.height(32.dp))
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(entries) { index, entry ->
                    CatalogueRow(
                        title = entry.title,
                        glyph = entry.glyph,
                        collapsed = collapsed,
                        selected = selected == index,
                        onFocused = { onSelect(index) },
                        onClick = { gridFocus.tryRequestFocus() },
                        modifier = Modifier.ifElse(selected == index, Modifier.focusRequester(selectedCatalogue)),
                    )
                }
            }
        }
        // Posters
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onFocusChanged { postersFocused = it.hasFocus }
                    .background(LiquidColors.Base.copy(alpha = .5f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
        ) {
            if (filter != null) {
                FilterByButton(
                    filterOptions = filter.options,
                    current = filter.current,
                    onFilterChange = filter.onChange,
                    getPossibleValues = filter.getValues,
                    alwaysExpanded = true,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = entries.getOrNull(selected)?.title.orEmpty(),
                color = LiquidColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Text(
                text = pluralStringResource(R.plurals.library_title_count, items.size, items.size),
                color = LiquidColors.TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
            )
            if (items.isNotEmpty()) {
                val widthPx = remember(density) { (100 * density).toInt() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp, start = 4.dp, end = 4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(count = items.size, key = { it }) { index ->
                        val item = items[index]
                        BrowsePoster(
                            item = item,
                            imageUrl = remember(item?.id) { images.posterUrl(item, widthPx * 2) },
                            onClick = { item?.let { onOpen(index, it) } },
                            onFocused = { item?.let { focusedItem = it } },
                            modifier = if (index == 0) Modifier.focusRequester(gridFocus) else Modifier,
                        )
                    }
                }
            } else {
                Text(
                    text = if (loading) stringResource(R.string.loading) else stringResource(R.string.browse_empty),
                    color = LiquidColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        // Details of the focused title, 12 dp from the posters like the 2 dp gap of the panels on its left plus the panel padding
        Spacer(Modifier.width(10.dp))
        Crossfade(
            targetState = shown,
            animationSpec = tween(250),
            label = "browse_details",
            modifier = Modifier.weight(1.3f).fillMaxHeight().onFocusChanged { detailsFocused = it.hasFocus },
        ) { item ->
            BrowseDetails(
                item = item,
                backdropUrl = remember(item?.id) { images.backdropUrl(item, 1200) },
                trailer = trailer?.takeIf { it.itemId == item?.id },
                trailerService = trailerService,
                muted = interfacePreferences.muteHeroTrailer,
                textScale = (interfacePreferences.detailsTextScale.takeIf { it > 0 } ?: 100) / 100f,
                onPlay = { item?.let(onPlay) },
            )
        }
    }
}

@Composable
internal fun CatalogueRow(
    title: String,
    glyph: String?,
    collapsed: Boolean,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                // The background is drawn below, the animated container color of the surface lags behind while scrolling
                containerColor = Color.Transparent,
                contentColor = if (selected) LiquidColors.Accent else LiquidColors.TextSecondary,
                focusedContainerColor = Color.Transparent,
                focusedContentColor = LiquidColors.Accent,
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, LiquidColors.Accent), shape = shape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(if (selected || focused) LiquidColors.Elevated else Color.Transparent, shape),
        ) {
            if (collapsed) {
                // The first characters of the name
                Text(
                    text = title.take(6).trimEnd(),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    if (glyph != null) Text(text = glyph, fontFamily = FontAwesome, fontSize = 12.sp, modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BrowsePoster(
    item: BaseItem?,
    imageUrl: String?,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val shape = RoundedCornerShape(8.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = ClickableSurfaceDefaults.shape(shape),
            colors =
                ClickableSurfaceDefaults.colors(
                    containerColor = LiquidColors.Elevated,
                    focusedContainerColor = LiquidColors.Elevated,
                ),
            border =
                ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(3.dp, LiquidColors.Accent), shape = shape),
                ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
            modifier = modifier.fillMaxWidth().aspectRatio(2f / 3f),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = item?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val badge = remember(item?.id) { item?.streamBadges()?.firstOrNull() }
                if (badge != null) {
                    Text(
                        text = badge,
                        color = LiquidColors.OnAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(LiquidColors.Accent, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
                val rating = item?.data?.communityRating
                if (rating != null && rating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(LiquidColors.Rail.copy(alpha = .85f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Icon(Icons.Default.Star, null, tint = LiquidColors.Accent, modifier = Modifier.size(8.dp))
                        Text(text = "%.1f".format(rating), color = LiquidColors.Accent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (item?.data?.userData?.played == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = LiquidColors.OnAccent,
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(14.dp)
                                .background(LiquidColors.Accent, CircleShape)
                                .padding(2.dp),
                    )
                }
            }
        }
        Text(
            text = item?.title.orEmpty(),
            color = if (focused) LiquidColors.Accent else LiquidColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item?.data?.productionYear?.toString().orEmpty(),
            color = LiquidColors.TextSecondary,
            fontSize = 10.sp,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowseDetails(
    item: BaseItem?,
    backdropUrl: String?,
    trailer: HeroTrailerSource?,
    trailerService: HeroTrailerService,
    muted: Boolean,
    textScale: Float,
    onPlay: () -> Unit,
) {
    val labelStyle = MaterialTheme.typography.labelMedium.let { it.copy(fontSize = it.fontSize * textScale) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(LiquidColors.Rail, RoundedCornerShape(12.dp)),
        ) {
            if (backdropUrl != null) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
            }
            if (trailer != null) {
                ZoomedTrailerPlayer(
                    trailer = trailer,
                    heroTrailerService = trailerService,
                    muted = muted,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
            }
            // Only the bottom edge blends into the panel, the rest of the picture keeps its brightness
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                .75f to Color.Transparent,
                                1f to LiquidColors.Rail.copy(alpha = .7f),
                            ),
                            RoundedCornerShape(12.dp),
                        ),
            )
        }
        if (item != null) {
            Text(
                text = (item.title ?: "").uppercase(),
                color = LiquidColors.TextPrimary,
                fontSize = 22.sp * textScale,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Year and ratings on one row, the time left or end time and the badges on the next, so nothing runs off the screen
            QuickDetails(
                details = item.ui.quickDetails,
                timeRemaining = null,
                textStyle = labelStyle,
            )
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                val context = LocalContext.current
                val endsAt =
                    remember(item.id) {
                        item.timeRemainingOrRuntime?.let {
                            DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + it.inWholeMilliseconds))
                        }
                    }
                if (endsAt != null) {
                    Text(
                        text = stringResource(R.string.ends_at, endsAt),
                        color = LiquidColors.TextPrimary,
                        style = labelStyle,
                    )
                }
                item.streamBadges().forEach { badge ->
                    Text(
                        text = badge,
                        color = LiquidColors.TextPrimary,
                        fontSize = 10.sp * textScale,
                        fontWeight = FontWeight.Bold,
                        modifier =
                            Modifier
                                .background(LiquidColors.Elevated, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.data.genres?.take(4)?.forEach { genre ->
                    Text(
                        text = genre,
                        color = LiquidColors.TextPrimary,
                        fontSize = 10.sp * textScale,
                        modifier =
                            Modifier
                                .background(LiquidColors.Elevated, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
            // The overview takes all the room between the genres and the Play button, which stays at the bottom
            val overview = item.data.overview
            if (overview.isNotNullOrBlank()) {
                Text(
                    text = overview,
                    color = LiquidColors.TextSecondary,
                    fontSize = 12.sp * textScale,
                    lineHeight = 17.sp * textScale,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            DetailsButton(
                text = if (item.resumeMs > 0) stringResource(R.string.resume) else stringResource(R.string.play),
                icon = Icons.Default.PlayArrow,
                primary = true,
                onClick = onPlay,
            )
        }
    }
}

@Composable
internal fun DetailsButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (primary) LiquidColors.Accent else LiquidColors.Elevated,
                contentColor = if (primary) LiquidColors.OnAccent else LiquidColors.TextPrimary,
                focusedContainerColor = if (primary) LiquidColors.Accent else LiquidColors.Elevated,
                focusedContentColor = if (primary) LiquidColors.OnAccent else LiquidColors.Accent,
            ),
        border =
            ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(1.dp, LiquidColors.Rim), shape = CircleShape),
                focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CircleShape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
