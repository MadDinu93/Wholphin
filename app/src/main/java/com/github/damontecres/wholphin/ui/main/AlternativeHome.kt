package com.github.damontecres.wholphin.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.data.RowColumn
import com.github.damontecres.wholphin.ui.detail.AlternativeBrowseLayout
import com.github.damontecres.wholphin.ui.detail.AlternativeBrowseViewModel
import com.github.damontecres.wholphin.ui.detail.BrowseEntry
import com.github.damontecres.wholphin.util.HomeRowLoadingState
import com.github.damontecres.wholphin.util.localizeServerName

/**
 * The Home page of the Alternative UI. It looks like the Movies page, only the catalogues of the second column are
 * the rows of the home page (continue watching, next up, latest in each library...) and the grid shows the focused row.
 */
@Composable
fun AlternativeHomeContent(
    homeRows: List<HomeRowLoadingState>,
    onClickItem: (RowColumn, BaseItem) -> Unit,
    onClickPlay: (BaseItem) -> Unit,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    images: AlternativeBrowseViewModel = hiltViewModel(key = "home"),
) {
    // Rows that are still loading, failed or are empty have nothing to show, the others keep their place in homeRows
    val rows =
        homeRows.withIndex().mapNotNull { (index, row) ->
            (row as? HomeRowLoadingState.Success)?.takeIf { it.items.isNotEmpty() }?.let { index to it }
        }
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val current = selected.coerceIn(0, (rows.size - 1).coerceAtLeast(0))
    val titles = rows.map { (_, row) -> row.title.getString() }
    val entries = remember(titles) { titles.map { BrowseEntry(localizeServerName(context, it), null) } }
    AlternativeBrowseLayout(
        heading = stringResource(R.string.home),
        entries = entries,
        selected = current,
        onSelect = { selected = it },
        items = rows.getOrNull(current)?.second?.items.orEmpty(),
        loading = homeRows.any { !it.completed },
        images = images,
        onOpen = { column, item -> rows.getOrNull(current)?.let { (rowIndex, _) -> onClickItem(RowColumn(rowIndex, column), item) } },
        onPlay = onClickPlay,
        preferences = preferences,
        modifier = modifier,
        showType = true,
    )
}
