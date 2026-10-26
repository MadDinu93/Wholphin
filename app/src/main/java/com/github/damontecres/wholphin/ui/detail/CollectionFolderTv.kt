package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.github.damontecres.wholphin.data.filter.DefaultTvFilterOptions
import com.github.damontecres.wholphin.data.model.CollectionFolderFilter
import com.github.damontecres.wholphin.data.model.GetItemsFilter
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.components.CollectionFolderView
import com.github.damontecres.wholphin.ui.components.ViewOptionsPoster
import com.github.damontecres.wholphin.ui.data.SeriesSortOptions
import com.github.damontecres.wholphin.ui.nav.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import org.jellyfin.sdk.model.api.BaseItemKind
import javax.inject.Inject

@HiltViewModel
class CollectionFolderTvViewModel
    @Inject
    constructor(
        val navigationManager: NavigationManager,
    ) : ViewModel()

/**
 * A TV library opens straight on its series, the genres and studios are in the filter
 */
@Composable
fun CollectionFolderTv(
    preferences: UserPreferences,
    destination: Destination.MediaItem,
    modifier: Modifier = Modifier,
    viewModel: CollectionFolderTvViewModel = hiltViewModel(),
) {
    CollectionFolderView(
        preferences = preferences,
        itemId = destination.itemId,
        initialFilter =
            CollectionFolderFilter(
                filter =
                    GetItemsFilter(
                        includeItemTypes = listOf(BaseItemKind.SERIES),
                    ),
            ),
        recursive = true,
        sortOptions = SeriesSortOptions,
        filterOptions = DefaultTvFilterOptions,
        defaultViewOptions = ViewOptionsPoster,
        modifier = modifier.fillMaxSize(),
        onClickItem = { _, item ->
            viewModel.navigationManager.navigateTo(item.destination())
        },
        playEnabled = false,
    )
}
