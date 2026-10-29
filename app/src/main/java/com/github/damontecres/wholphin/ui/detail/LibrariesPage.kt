package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.destination
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.cards.GenreCard
import com.github.damontecres.wholphin.ui.cards.LocalPosterCardShape
import com.github.damontecres.wholphin.ui.cards.glassHighlight
import com.github.damontecres.wholphin.ui.cards.posterCardBorder
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.main.settings.Library
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.tryRequestFocus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import javax.inject.Inject

/**
 * How many titles a section of the Library page holds, and the newest one for its picture
 */
data class LibrarySectionPreview(
    val count: Int? = null,
    val newest: BaseItem? = null,
)

data class LibrariesPageState(
    val movies: LibrarySectionPreview = LibrarySectionPreview(),
    val series: LibrarySectionPreview = LibrarySectionPreview(),
)

@HiltViewModel
class LibrariesViewModel
    @Inject
    constructor(
        navDrawerService: NavDrawerService,
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
        private val imageUrlService: ImageUrlService,
        val navigationManager: NavigationManager,
    ) : ViewModel() {
        val state = navDrawerService.state

        private val _sections = MutableStateFlow(LibrariesPageState())
        val sections: StateFlow<LibrariesPageState> = _sections

        init {
            viewModelScope.launchIO {
                val movies = loadPreview(BaseItemKind.MOVIE)
                _sections.update { it.copy(movies = movies) }
            }
            viewModelScope.launchIO {
                val series = loadPreview(BaseItemKind.SERIES)
                _sections.update { it.copy(series = series) }
            }
        }

        private suspend fun loadPreview(type: BaseItemKind): LibrarySectionPreview =
            try {
                val result =
                    api.itemsApi
                        .getItems(
                            userId = serverRepository.currentUser?.id,
                            includeItemTypes = listOf(type),
                            recursive = true,
                            sortBy = listOf(ItemSortBy.DATE_CREATED),
                            sortOrder = listOf(SortOrder.DESCENDING),
                            enableImageTypes = listOf(ImageType.BACKDROP),
                            imageTypeLimit = 1,
                            limit = 1,
                            enableTotalRecordCount = true,
                        ).content
                LibrarySectionPreview(
                    count = result.totalRecordCount,
                    newest = result.items.firstOrNull()?.let(::BaseItem),
                )
            } catch (ex: Exception) {
                Timber.e(ex, "Error getting the Library preview for %s", type)
                LibrarySectionPreview()
            }

        fun imageUrl(
            library: Library,
            widthPx: Int,
        ): String? =
            imageUrlService.getItemImageUrl(
                itemId = library.itemId,
                imageType = ImageType.PRIMARY,
                fillWidth = widthPx,
            )

        fun backdropUrl(
            item: BaseItem?,
            widthPx: Int,
        ): String? = imageUrlService.getItemImageUrl(item, ImageType.BACKDROP, fillWidth = widthPx)

        fun open(library: Library) {
            navigationManager.navigateTo(library.destination())
        }

        fun open(destination: Destination) {
            navigationManager.navigateTo(destination)
        }
    }

/**
 * The Library page: one card for the server's catalogs, one for all the movies and one for all the series
 */
@Composable
fun LibrariesPage(
    modifier: Modifier = Modifier,
    viewModel: LibrariesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val libraries = state.allLibraries

    val columns = 3
    val spacing = 24.dp
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val cardWidthPx =
        remember(density) {
            with(density) {
                (configuration.screenWidthDp.dp - (2 * 16.dp + (columns - 1) * spacing))
                    .div(columns)
                    .roundToPx()
            }
        }
    // Back from a section lands on the card it was opened from
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    val focusRequesters = remember { List(columns) { FocusRequester() } }
    LaunchedEffect(Unit) { focusRequesters[focusedIndex].tryRequestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
    ) {
        val totalTitles =
            sections.movies.count
                ?.let { movies -> sections.series.count?.let { movies + it } }
        LibraryTitle(
            title = stringResource(R.string.library),
            subtitle = totalTitles?.let { pluralStringResource(R.plurals.library_title_count, it, it) },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val catalogImage =
                remember(libraries, cardWidthPx) {
                    libraries.firstOrNull()?.let { viewModel.imageUrl(it, cardWidthPx) }
                }
            LibrarySectionCard(
                title = stringResource(R.string.server_catalogs),
                subtitle =
                    libraries
                        .takeIf { it.isNotEmpty() }
                        ?.let { pluralStringResource(R.plurals.library_catalog_count, it.size, it.size) },
                imageUrl = catalogImage,
                onClick = {
                    focusedIndex = 0
                    viewModel.open(Destination.ServerCatalogs)
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[0]),
            )
            LibrarySectionCard(
                title = stringResource(R.string.movies_title),
                subtitle =
                    sections.movies.count?.let {
                        pluralStringResource(R.plurals.library_title_count, it, it)
                    },
                imageUrl =
                    remember(sections.movies.newest, cardWidthPx) {
                        viewModel.backdropUrl(sections.movies.newest, cardWidthPx)
                    },
                onClick = {
                    focusedIndex = 1
                    viewModel.open(Destination.AllMovies)
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[1]),
            )
            LibrarySectionCard(
                title = stringResource(R.string.tv_shows_title),
                subtitle =
                    sections.series.count?.let {
                        pluralStringResource(R.plurals.library_title_count, it, it)
                    },
                imageUrl =
                    remember(sections.series.newest, cardWidthPx) {
                        viewModel.backdropUrl(sections.series.newest, cardWidthPx)
                    },
                onClick = {
                    focusedIndex = 2
                    viewModel.open(Destination.AllSeries)
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequesters[2]),
            )
        }
    }
}

@Composable
private fun LibrarySectionCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CardDefaults.shape(LocalPosterCardShape.current),
        border = posterCardBorder(),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        colors = CardDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(AspectRatios.WIDE)
                    .clip(LocalPosterCardShape.current)
                    .glassHighlight(interactionSource),
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Keeps the title readable over any picture
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                .45f to Color.Black.copy(alpha = .15f),
                                1f to Color.Black.copy(alpha = .8f),
                            ),
                        ),
            )
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = .75f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Every library the user can see, as a grid of cards. On Remux each catalog of an addon is a library, so a
 * server often has dozens of them.
 */
@Composable
fun ServerCatalogsPage(
    modifier: Modifier = Modifier,
    viewModel: LibrariesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val libraries = state.allLibraries
    if (libraries.isEmpty()) {
        LoadingPage(modifier)
        return
    }

    val columns = 4
    val spacing = 16.dp
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val cardWidthPx =
        remember(density) {
            with(density) {
                (configuration.screenWidthDp.dp - (2 * 16.dp + 3 * spacing))
                    .div(columns)
                    .roundToPx()
            }
        }
    // Back from a catalog lands on the card it was opened from
    var focusedIndex by rememberSaveable { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier.padding(top = 24.dp),
    ) {
        LibraryTitle(
            title = stringResource(R.string.server_catalogs),
            subtitle = pluralStringResource(R.plurals.library_catalog_count, libraries.size, libraries.size),
            modifier = Modifier.padding(start = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = rememberLazyGridState(initialFirstVisibleItemIndex = focusedIndex),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(libraries, key = { _, library -> library.itemId }) { index, library ->
                val imageUrl = remember(library, cardWidthPx) { viewModel.imageUrl(library, cardWidthPx) }
                GenreCard(
                    genreId = library.itemId,
                    name = library.name,
                    imageUrl = imageUrl,
                    onClick = {
                        focusedIndex = index
                        viewModel.open(library)
                    },
                    onLongClick = {},
                    modifier =
                        if (index == focusedIndex.coerceIn(libraries.indices)) {
                            Modifier.focusRequester(focusRequester)
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
}
