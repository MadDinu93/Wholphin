package com.github.damontecres.wholphin.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
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
import com.github.damontecres.wholphin.ui.FontAwesome
import com.github.damontecres.wholphin.ui.components.TimeDisplay
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.preferences.PreferenceScreenOption
import com.github.damontecres.wholphin.ui.setup.UserIconCardImage
import com.github.damontecres.wholphin.ui.tryRequestFocus

/**
 * Lets a page tell [TopNavigation] whether it is scrolled to the top, so the bar can get out of the way while browsing
 */
@Stable
class TopNavBarState {
    var contentAtTop by mutableStateOf(true)
}

/**
 * Provided while a page is shown below the [TopNavBar], null otherwise (e.g. with the side drawer)
 */
val LocalTopNavBarState = compositionLocalOf<TopNavBarState?> { null }

/** Vertical space the bar takes above the page content */
val TopNavBarHeight = 76.dp

/** Distance of the cards and the bar from both screen edges, so the pages are centered on the screen */
private val TopNavContentHorizontalPadding = 32.dp

/** Rows and grids add this much of their own at the start, so it is left out there to keep both sides equal */
private val TopNavContentStartInset = 16.dp

/**
 * Apple TV style navigation: a row of tabs at the top of the root pages instead of the side drawer
 *
 * The bar is only shown on pages reachable from it (home, search, libraries, pinned items). Other pages such as details
 * get the full screen and are left with back.
 */
@Composable
fun TopNavigation(
    destination: Destination,
    preferences: UserPreferences,
    user: JellyfinUser,
    server: JellyfinServer,
    onClearBackdrop: () -> Unit,
    viewModel: NavDrawerViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { viewModel.updateSelectedIndex() }
    val serviceState by viewModel.serviceState.collectAsState()
    val state by viewModel.state.collectAsState()
    val selectedIndex = state.selectedIndex

    val showBar =
        remember(destination, serviceState.items, serviceState.allLibraries) {
            when (destination) {
                is Destination.Home,
                is Destination.Search,
                Destination.Libraries,
                Destination.ServerCatalogs,
                Destination.AllMovies,
                Destination.AllSeries,
                Destination.Favorites,
                Destination.Discover,
                -> true

                // A catalog opened from the Library page
                is Destination.MediaItem -> {
                    serviceState.allLibraries.any { it.itemId == destination.itemId }
                }

                else -> serviceState.items.any { it is ServerNavDrawerItem && it.destination == destination }
            }
        }

    val barState = remember { TopNavBarState() }
    // A page that hid the bar while scrolled down must not keep it hidden on the next page
    LaunchedEffect(destination) { barState.contentAtTop = true }
    var barFocused by remember { mutableStateOf(false) }
    val barVisible = showBar && (barFocused || barState.contentAtTop)
    val topPadding = animateDpAsState(if (barVisible) TopNavBarHeight else 0.dp, label = "top_nav_padding")
    val barProgress = animateFloatAsState(if (barVisible) 1f else 0f, label = "top_nav_visible")

    val selectedFocusRequester = remember { FocusRequester() }
    val homeFocusRequester = remember { FocusRequester() }
    val focusBar = {
        if (!selectedFocusRequester.tryRequestFocus()) {
            homeFocusRequester.tryRequestFocus()
        }
    }

    // Like the drawer, back on the home page first moves to the navigation, another back press quits the app
    BackHandler(enabled = showBar && !barFocused && destination is Destination.Home) {
        focusBar()
    }

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalTopNavBarState provides if (showBar) barState else null) {
            DestinationContent(
                destination = destination,
                preferences = preferences,
                onClearBackdrop = onClearBackdrop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            start = TopNavContentHorizontalPadding - TopNavContentStartInset,
                            end = TopNavContentHorizontalPadding,
                        )
                        // Measured once for where the bar ends up, only the placement follows the animation, so the
                        // page is not measured again every frame while the bar slides
                        .layout { measurable, constraints ->
                            val targetTop = if (barVisible) TopNavBarHeight.roundToPx() else 0
                            val placeable = measurable.measure(constraints.offset(vertical = -targetTop))
                            layout(placeable.width, placeable.height + targetTop) {
                                placeable.place(0, topPadding.value.roundToPx())
                            }
                        },
            )
        }
        if (showBar) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = barProgress.value
                            translationY = -(1f - barProgress.value) * size.height
                        }.padding(start = TopNavContentHorizontalPadding, end = TopNavContentHorizontalPadding, top = 16.dp, bottom = 8.dp)
                        .onFocusChanged { barFocused = it.hasFocus }
                        .focusGroup()
                        .focusProperties {
                            onEnter = { focusBar() }
                        },
            ) {
                val userImageUrl = remember(user) { viewModel.getUserImage(user) }
                Surface(
                    onClick = { viewModel.navigateToSetup(SetupDestination.UserList(server)) },
                    shape = ClickableSurfaceDefaults.shape(CircleShape),
                    colors =
                        ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                        ),
                    border =
                        ClickableSurfaceDefaults.border(
                            focusedBorder =
                                Border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                                    shape = CircleShape,
                                ),
                        ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
                ) {
                    UserIconCardImage(
                        id = user.id,
                        name = user.name,
                        imageUrl = userImageUrl,
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .6f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = .12f), CircleShape)
                            .clip(CircleShape)
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp),
                ) {
                    TopNavTab(
                        text = stringResource(R.string.home),
                        selected = selectedIndex == HOME_INDEX,
                        onClick = {
                            viewModel.setIndex(HOME_INDEX)
                            if (destination is Destination.Home) {
                                viewModel.navigationManager.reloadHome()
                                onClearBackdrop.invoke()
                            } else {
                                viewModel.navigationManager.goToHome()
                            }
                        },
                        icon = Icons.Default.Home,
                        modifier =
                            Modifier
                                .focusRequester(homeFocusRequester)
                                .ifElse(selectedIndex == HOME_INDEX, Modifier.focusRequester(selectedFocusRequester)),
                    )
                    if (serviceState.nowPlayingEnabled) {
                        TopNavTab(
                            text = stringResource(R.string.now_playing),
                            selected = selectedIndex == NOW_PLAYING_INDEX,
                            onClick = {
                                viewModel.setIndex(NOW_PLAYING_INDEX)
                                viewModel.navigationManager.navigateTo(Destination.NowPlaying)
                            },
                            icon = Icons.Default.PlayArrow,
                            modifier =
                                Modifier.ifElse(
                                    selectedIndex == NOW_PLAYING_INDEX,
                                    Modifier.focusRequester(selectedFocusRequester),
                                ),
                        )
                    }
                    val context = LocalContext.current
                    serviceState.items.forEachIndexed { index, item ->
                        TopNavTab(
                            text = item.name(context),
                            selected = selectedIndex == index,
                            onClick = { viewModel.onClickDrawerItem(index, item) },
                            iconContent = { NavDrawerItemIcon(item) },
                            modifier =
                                Modifier.ifElse(
                                    selectedIndex == index,
                                    Modifier.focusRequester(selectedFocusRequester),
                                ),
                        )
                    }
                    TopNavTab(
                        text = stringResource(R.string.search),
                        selected = selectedIndex == SEARCH_INDEX,
                        onClick = {
                            viewModel.setIndex(SEARCH_INDEX)
                            viewModel.navigationManager.navigateToFromDrawer(Destination.Search())
                        },
                        icon = Icons.Default.Search,
                        modifier =
                            Modifier.ifElse(
                                selectedIndex == SEARCH_INDEX,
                                Modifier.focusRequester(selectedFocusRequester),
                            ),
                    )
                    TopNavTab(
                        text = stringResource(R.string.settings),
                        selected = false,
                        onClick = {
                            viewModel.navigationManager.navigateTo(
                                Destination.Settings(PreferenceScreenOption.BASIC),
                            )
                        },
                        icon = Icons.Default.Settings,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Balances the profile button so the tabs stay centered
                Spacer(Modifier.width(44.dp))
            }
        }
        if (preferences.appPreferences.interfacePreferences.showClock) {
            TimeDisplay()
        }
    }
}

@Composable
private fun TopNavTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) = TopNavTab(
    text = text,
    selected = selected,
    onClick = onClick,
    iconContent = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
    },
    modifier = modifier,
)

@Composable
private fun TopNavTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (selected) onSurface.copy(alpha = .18f) else Color.Transparent,
                contentColor = if (selected) onSurface else onSurface.copy(alpha = .75f),
                focusedContainerColor = onSurface,
                focusedContentColor = MaterialTheme.colorScheme.surface,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            iconContent()
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NavDrawerItemIcon(item: NavDrawerItem) {
    val icon = remember(item) { item.iconRes }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
        if (item.iconIsFont) {
            Text(
                text = stringResource(icon),
                fontSize = 15.sp,
                fontFamily = FontAwesome,
                color = LocalContentColor.current,
            )
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
