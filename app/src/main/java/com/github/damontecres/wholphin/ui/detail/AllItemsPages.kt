package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.filter.DefaultTvFilterOptions
import com.github.damontecres.wholphin.data.model.CollectionFolderFilter
import com.github.damontecres.wholphin.data.model.GetItemsFilter
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.components.CollectionFolderView
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.GridClickActions
import com.github.damontecres.wholphin.ui.components.TabDetails
import com.github.damontecres.wholphin.ui.components.TabbedPage
import com.github.damontecres.wholphin.ui.components.ViewOptionsPoster
import com.github.damontecres.wholphin.ui.data.MovieSortOptions
import com.github.damontecres.wholphin.ui.data.SeriesSortOptions
import com.github.damontecres.wholphin.ui.data.VideoSortOptions
import com.github.damontecres.wholphin.ui.theme.LocalLiquidStyle
import org.jellyfin.sdk.model.api.BaseItemKind

/*
 * These grids have no library above them, so their view model gets a key that is not an item ID. The items
 * are then asked for across the whole server, and the sort and filter are saved under that key.
 */
private const val ALL_MOVIES_KEY = "all_movies"
private const val ALL_COLLECTIONS_KEY = "all_collections"
private const val ALL_SERIES_KEY = "all_series"

/**
 * The movies of every library together, with the collections in a second tab
 */
@Composable
fun AllMoviesPage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: LibrariesViewModel = hiltViewModel(),
) {
    val tabs =
        remember {
            listOf(
                TabDetails(R.string.movies_title),
                TabDetails(R.string.collections),
            )
        }
    val actions =
        remember {
            GridClickActions(onClickItem = { _, item -> viewModel.open(item.destination()) })
        }
    var showHeader by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier) {
        // Thorphin: the Modern and Squared styles open the page with a large title and the number of movies
        if (LocalLiquidStyle.current != null && showHeader) {
            val count = viewModel.sections.collectAsState().value.movies.count
            LibraryTitle(
                title = stringResource(R.string.movies_title),
                subtitle = count?.let { pluralStringResource(R.plurals.library_title_count, it, it) },
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        TabbedPage(
            itemId = ALL_MOVIES_KEY,
            tabs = tabs,
            modifier = Modifier.weight(1f),
            showTabs = showHeader,
        ) { tabIndex, tabDetails ->
            when (tabIndex) {
                0 -> {
                    CollectionFolderView(
                        preferences = preferences,
                        itemId = ALL_MOVIES_KEY,
                        initialFilter =
                            CollectionFolderFilter(
                                filter = GetItemsFilter(includeItemTypes = listOf(BaseItemKind.MOVIE)),
                            ),
                        recursive = true,
                        actions = actions,
                        sortOptions = MovieSortOptions,
                        // Playing all needs a library to play from
                        playEnabled = false,
                        defaultViewOptions = ViewOptionsPoster,
                        showTitle = false,
                        positionCallback = { columns, position ->
                            showHeader = position < columns
                        },
                        focusRequesterOnEmpty = tabDetails.tabFocusRequester,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .focusRequester(tabDetails.contentFocusRequester),
                    )
                }

                1 -> {
                    CollectionFolderView(
                        preferences = preferences,
                        itemId = ALL_COLLECTIONS_KEY,
                        initialFilter =
                            CollectionFolderFilter(
                                filter = GetItemsFilter(includeItemTypes = listOf(BaseItemKind.BOX_SET)),
                            ),
                        recursive = true,
                        actions = actions,
                        sortOptions = VideoSortOptions,
                        playEnabled = false,
                        defaultViewOptions = ViewOptionsPoster,
                        showTitle = false,
                        positionCallback = { columns, position ->
                            showHeader = position < columns
                        },
                        focusRequesterOnEmpty = tabDetails.tabFocusRequester,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .focusRequester(tabDetails.contentFocusRequester),
                    )
                }

                else -> {
                    ErrorMessage("Invalid tab index $tabIndex", null)
                }
            }
        }
    }
}

/**
 * The series of every library together
 */
@Composable
fun AllSeriesPage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: LibrariesViewModel = hiltViewModel(),
) {
    val actions =
        remember {
            GridClickActions(onClickItem = { _, item -> viewModel.open(item.destination()) })
        }
    CollectionFolderView(
        preferences = preferences,
        itemId = ALL_SERIES_KEY,
        initialFilter =
            CollectionFolderFilter(
                nameOverride = stringResource(R.string.tv_shows_title),
                filter = GetItemsFilter(includeItemTypes = listOf(BaseItemKind.SERIES)),
            ),
        recursive = true,
        actions = actions,
        sortOptions = SeriesSortOptions,
        filterOptions = DefaultTvFilterOptions,
        playEnabled = false,
        defaultViewOptions = ViewOptionsPoster,
        modifier = modifier.fillMaxSize(),
    )
}
