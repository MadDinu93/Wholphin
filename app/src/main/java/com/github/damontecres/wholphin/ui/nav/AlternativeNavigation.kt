package com.github.damontecres.wholphin.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.JellyfinServer
import com.github.damontecres.wholphin.data.model.JellyfinUser
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.SetupDestination
import com.github.damontecres.wholphin.services.destination
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.preferences.PreferenceScreenOption
import com.github.damontecres.wholphin.ui.setup.UserIconCardImage
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.LocalHomeScreenStyle
import com.github.damontecres.wholphin.ui.theme.isMinimalist
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.LocalClock
import org.jellyfin.sdk.model.api.CollectionType

/** Width of the rail, as in the Alternative UI design */
val AlternativeRailWidth = 72.dp

/** The entries of the rail the page being shown can belong to */
private enum class RailEntry { Home, Catalog, Movies, Series, Sets, Lists, Favorites, Discover, NowPlaying, Search, Settings }

/** How an entry of the rail is drawn: a FontAwesome glyph or a vector */
private sealed interface RailIcon {
    data class Glyph(
        val res: Int,
    ) : RailIcon

    data class Vector(
        val vector: ImageVector,
    ) : RailIcon
}

/**
 * The Alternative UI navigation: a permanent rail on the left (Home, Catalog, Movies, Series, Sets, Lists, search and
 * settings) with the page next to it. There is no "Library" entry, the Catalog, Movies and Series entries replace it.
 *
 * Like the top bar, the rail only shows on root pages, details and other pages get the whole screen.
 */
@Composable
fun AlternativeNavigation(
    destination: Destination,
    preferences: UserPreferences,
    user: JellyfinUser,
    server: JellyfinServer,
    onClearBackdrop: () -> Unit,
    viewModel: NavDrawerViewModel,
    modifier: Modifier = Modifier,
) {
    val serviceState by viewModel.serviceState.collectAsState()
    val libraries = serviceState.allLibraries
    val setsLibrary = remember(libraries) { libraries.firstOrNull { it.collectionType == CollectionType.BOXSETS } }
    val listsLibrary = remember(libraries) { libraries.firstOrNull { it.collectionType == CollectionType.PLAYLISTS } }
    val hasFavorites = serviceState.items.any { it is NavDrawerItem.Favorites }
    val hasDiscover = serviceState.items.any { it is NavDrawerItem.Discover }

    val selected: RailEntry? =
        remember(destination, setsLibrary, listsLibrary) {
            when (destination) {
                is Destination.Home -> RailEntry.Home
                is Destination.Search -> RailEntry.Search
                Destination.NowPlaying -> RailEntry.NowPlaying
                is Destination.Settings -> RailEntry.Settings
                Destination.Favorites -> RailEntry.Favorites
                Destination.Discover -> RailEntry.Discover
                Destination.AllMovies -> RailEntry.Movies
                Destination.AllSeries -> RailEntry.Series
                Destination.Libraries, Destination.ServerCatalogs -> RailEntry.Catalog
                is Destination.MediaItem ->
                    when (destination.itemId) {
                        setsLibrary?.itemId -> RailEntry.Sets
                        listsLibrary?.itemId -> RailEntry.Lists
                        else -> if (libraries.any { it.itemId == destination.itemId }) RailEntry.Catalog else null
                    }

                else -> null
            }
        }
    // A page which is not reached from the rail gets the whole screen
    val showRail = selected != null

    val selectedFocusRequester = remember { FocusRequester() }
    val homeFocusRequester = remember { FocusRequester() }
    val focusRail = {
        if (!selectedFocusRequester.tryRequestFocus()) {
            homeFocusRequester.tryRequestFocus()
        }
    }
    var railFocused by remember { mutableStateOf(false) }
    // Like the drawer, back on the home page first moves to the navigation, another back press quits the app
    BackHandler(enabled = showRail && !railFocused && destination is Destination.Home) { focusRail() }

    val navigation = viewModel.navigationManager
    val go: (RailEntry) -> Unit = { entry ->
        when (entry) {
            RailEntry.Home -> {
                if (destination is Destination.Home) {
                    navigation.reloadHome()
                    onClearBackdrop.invoke()
                } else {
                    navigation.goToHome()
                }
            }

            RailEntry.Catalog -> navigation.navigateToFromDrawer(Destination.ServerCatalogs)

            RailEntry.Movies -> navigation.navigateToFromDrawer(Destination.AllMovies)

            RailEntry.Series -> navigation.navigateToFromDrawer(Destination.AllSeries)

            RailEntry.Sets -> setsLibrary?.let { navigation.navigateToFromDrawer(it.destination()) }

            RailEntry.Lists -> listsLibrary?.let { navigation.navigateToFromDrawer(it.destination()) }

            RailEntry.Favorites -> navigation.navigateToFromDrawer(Destination.Favorites)

            RailEntry.Discover -> navigation.navigateToFromDrawer(Destination.Discover)

            RailEntry.NowPlaying -> navigation.navigateTo(Destination.NowPlaying)

            RailEntry.Settings -> navigation.navigateTo(Destination.Settings(PreferenceScreenOption.BASIC))

            RailEntry.Search -> navigation.navigateToFromDrawer(Destination.Search())
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .width(AlternativeRailWidth)
                        .fillMaxHeight()
                        .background(LiquidColors.Rail)
                        .onFocusChanged { railFocused = it.hasFocus }
                        .focusGroup()
                        .focusProperties { onEnter = { focusRail() } }
                        .padding(vertical = 16.dp, horizontal = 6.dp),
            ) {
                val userImageUrl = remember(user) { viewModel.getUserImage(user) }
                Surface(
                    onClick = { viewModel.navigateToSetup(SetupDestination.UserList(server)) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
                    colors =
                        ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                        ),
                    border =
                        ClickableSurfaceDefaults.border(
                            focusedBorder =
                                Border(
                                    BorderStroke(2.dp, LiquidColors.Accent),
                                    shape = RoundedCornerShape(50),
                                ),
                        ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                ) {
                    UserIconCardImage(
                        id = user.id,
                        name = user.name,
                        imageUrl = userImageUrl,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                // The entries are centered between the profile and Settings, and scroll if there are too many
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            // Room for the focused entry to grow and show its border, the scroll area would cut it
                            .padding(vertical = 8.dp),
                ) {
                    @Composable
                    fun entry(
                        which: RailEntry,
                        label: String,
                        icon: RailIcon,
                    ) = RailItem(
                        label = label,
                        icon = icon,
                        selected = selected == which,
                        onClick = { go(which) },
                        modifier =
                            Modifier
                                .ifElse(which == RailEntry.Home, Modifier.focusRequester(homeFocusRequester))
                                .ifElse(selected == which, Modifier.focusRequester(selectedFocusRequester)),
                    )
                    entry(RailEntry.Home, stringResource(R.string.home), RailIcon.Glyph(R.string.fa_house))
                    entry(RailEntry.Movies, stringResource(R.string.nav_movies), RailIcon.Glyph(R.string.fa_film))
                    entry(RailEntry.Series, stringResource(R.string.nav_series), RailIcon.Glyph(R.string.fa_tv))
                    if (setsLibrary != null) {
                        entry(RailEntry.Sets, stringResource(R.string.nav_sets), RailIcon.Glyph(R.string.fa_open_folder))
                    }
                    if (listsLibrary != null) {
                        entry(RailEntry.Lists, stringResource(R.string.nav_lists), RailIcon.Glyph(R.string.fa_list_ul))
                    }
                    if (hasFavorites) {
                        entry(RailEntry.Favorites, stringResource(R.string.favorites), RailIcon.Glyph(R.string.fa_heart))
                    }
                    if (hasDiscover) {
                        entry(RailEntry.Discover, stringResource(R.string.discover), RailIcon.Glyph(R.string.fa_magnifying_glass_plus))
                    }
                    if (serviceState.nowPlayingEnabled) {
                        entry(RailEntry.NowPlaying, stringResource(R.string.now_playing), RailIcon.Vector(Icons.Default.PlayArrow))
                    }
                    entry(RailEntry.Search, stringResource(R.string.search), RailIcon.Vector(Icons.Default.Search))
                }
                }
                RailItem(
                    label = stringResource(R.string.settings),
                    icon = RailIcon.Vector(Icons.Default.Settings),
                    selected = selected == RailEntry.Settings,
                    onClick = { go(RailEntry.Settings) },
                    modifier = Modifier.ifElse(selected == RailEntry.Settings, Modifier.focusRequester(selectedFocusRequester)),
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            CompositionLocalProvider(LocalTopNavBarState provides null) {
                DestinationContent(
                    destination = destination,
                    preferences = preferences,
                    onClearBackdrop = onClearBackdrop,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            // Thorphin: the Minimalist designs have the same 8 dp margin at the top as at the sides
                            .padding(
                                start = if (showRail) 8.dp else 0.dp,
                                end = 8.dp,
                                top = if (showRail) (if (LocalHomeScreenStyle.current.isMinimalist) 8.dp else 12.dp) else 0.dp,
                            ),
                )
            }
            if (showRail && preferences.appPreferences.interfacePreferences.showClock) {
                AlternativeClock(modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 16.dp))
            }
        }
    }
}

/** One entry of the rail: icon over a small label, filled amber while it is the page shown */
@Composable
private fun RailItem(
    label: String,
    icon: RailIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (selected) LiquidColors.Accent else Color.Transparent,
                contentColor = if (selected) LiquidColors.OnAccent else LiquidColors.TextSecondary,
                focusedContainerColor = if (selected) LiquidColors.Accent else LiquidColors.Elevated,
                focusedContentColor = if (selected) LiquidColors.OnAccent else LiquidColors.TextPrimary,
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder =
                    Border(
                        BorderStroke(2.dp, LiquidColors.Accent),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        ) {
            RailGlyph(icon, 20.dp)
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = Color.Unspecified,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RailGlyph(
    icon: RailIcon,
    size: Dp,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        when (icon) {
            is RailIcon.Glyph -> {
                Text(
                    text = stringResource(icon.res),
                    fontSize = 15.sp,
                    fontFamily = FontAwesome,
                    color = LocalContentColor.current,
                )
            }

            is RailIcon.Vector -> {
                Icon(
                    imageVector = icon.vector,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** The clock, floating over the top right corner of the page */
@Composable
private fun AlternativeClock(modifier: Modifier = Modifier) {
    val timeString by LocalClock.current.timeString
    Text(
        text = timeString,
        color = LiquidColors.TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            modifier
                .background(LiquidColors.Rail.copy(alpha = .7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
