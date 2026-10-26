package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.data.model.CollectionFolderFilter
import com.github.damontecres.wholphin.data.model.GetItemsFilter
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.components.CollectionFolderView
import com.github.damontecres.wholphin.ui.components.ViewOptionsPoster
import com.github.damontecres.wholphin.ui.data.MovieSortOptions
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.preferences.PreferencesViewModel
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * A movie library opens straight on its movies, the genres are in the filter
 */
@Composable
fun CollectionFolderMovie(
    preferences: UserPreferences,
    destination: Destination.MediaItem,
    modifier: Modifier = Modifier,
    preferencesViewModel: PreferencesViewModel = hiltViewModel(),
) {
    CollectionFolderView(
        preferences = preferences,
        onClickItem = { _, item ->
            preferencesViewModel.navigationManager.navigateTo(item.destination())
        },
        itemId = destination.itemId,
        viewModelKey = "${destination.itemId}_library",
        initialFilter =
            CollectionFolderFilter(
                filter =
                    GetItemsFilter(
                        includeItemTypes = listOf(BaseItemKind.MOVIE),
                    ),
            ),
        recursive = true,
        sortOptions = MovieSortOptions,
        defaultViewOptions = ViewOptionsPoster,
        modifier = modifier.fillMaxSize(),
        playEnabled = true,
    )
}
