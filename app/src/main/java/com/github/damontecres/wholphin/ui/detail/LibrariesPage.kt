package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.services.ImageUrlService
import com.github.damontecres.wholphin.services.NavDrawerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.destination
import com.github.damontecres.wholphin.ui.cards.GenreCard
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.main.settings.Library
import com.github.damontecres.wholphin.ui.tryRequestFocus
import dagger.hilt.android.lifecycle.HiltViewModel
import org.jellyfin.sdk.model.api.ImageType
import javax.inject.Inject

@HiltViewModel
class LibrariesViewModel
    @Inject
    constructor(
        navDrawerService: NavDrawerService,
        private val imageUrlService: ImageUrlService,
        val navigationManager: NavigationManager,
    ) : ViewModel() {
        val state = navDrawerService.state

        fun imageUrl(
            library: Library,
            widthPx: Int,
        ): String? =
            imageUrlService.getItemImageUrl(
                itemId = library.itemId,
                imageType = ImageType.PRIMARY,
                fillWidth = widthPx,
            )

        fun open(library: Library) {
            navigationManager.navigateTo(library.destination())
        }
    }

/**
 * Every library the user can see, as a grid of cards. The nav drawer links here instead of listing
 * each library, which on a server with many catalogs pushed the rest of the drawer off screen.
 */
@Composable
fun LibrariesPage(
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.tryRequestFocus() }

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier.padding(top = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.library),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
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
                    onClick = { viewModel.open(library) },
                    onLongClick = {},
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                )
            }
        }
    }
}
