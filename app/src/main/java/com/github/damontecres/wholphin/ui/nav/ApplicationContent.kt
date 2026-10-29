package com.github.damontecres.wholphin.ui.nav

import com.github.damontecres.wholphin.ui.theme.isLiquid
import com.github.damontecres.wholphin.preferences.BackdropStyle
import com.github.damontecres.wholphin.preferences.HomeScreenStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.rememberDrawerState
import com.github.damontecres.wholphin.data.model.JellyfinServer
import com.github.damontecres.wholphin.data.model.JellyfinUser
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.BackdropService
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.services.ThemeSongPlayer
import com.github.damontecres.wholphin.ui.cards.LocalCardProgressStyle
import com.github.damontecres.wholphin.ui.cards.LocalShowCardInfo
import com.github.damontecres.wholphin.ui.preferences.PreferenceScreenOption
import com.github.damontecres.wholphin.ui.theme.isAlternative
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.LocalHomeScreenStyle
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Top scrim configuration for text readability (clock, season tabs)
const val TOP_SCRIM_ALPHA = 0.55f
const val TOP_SCRIM_END_FRACTION = 0.25f // Fraction of backdrop image height

@HiltViewModel
class ApplicationContentViewModel
    @Inject
    constructor(
        val backdropService: BackdropService,
        val heroTrailerService: HeroTrailerService,
        val themeSongPlayer: ThemeSongPlayer,
    ) : ViewModel() {
        fun clearBackdrop() {
            viewModelScope.launchIO { backdropService.clearBackdrop() }
        }
    }

/**
 * This is generally the root composable of the of the app
 *
 * Here the navigation backstack is used and pages are rendered in the nav drawer or full screen
 */
@Composable
fun ApplicationContent(
    server: JellyfinServer,
    user: JellyfinUser,
    navigationManager: NavigationManager,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    enableTopScrim: Boolean = true,
    viewModel: ApplicationContentViewModel = hiltViewModel(),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    // Thorphin: the Alternative UI has its own amber palette, written before any page reads it
    val homeScreenStyle = preferences.appPreferences.interfacePreferences.homeScreenStyle
    homeScreenStyle.isAlternative.let {
        if (LiquidColors.alternative != it) LiquidColors.alternative = it
    }
    Box(
        modifier = modifier,
    ) {
        val chosenBackdropStyle = preferences.appPreferences.interfacePreferences.backdropStyle
        // Thorphin: the Modern and Squared designs have a neutral black background, the colors taken from the
        // picture would tint it purple and blue
        val backdropStyle =
            if (preferences.appPreferences.interfacePreferences.homeScreenStyle.isLiquid &&
                (chosenBackdropStyle == BackdropStyle.BACKDROP_DYNAMIC_COLOR || chosenBackdropStyle == BackdropStyle.UNRECOGNIZED)
            ) {
                BackdropStyle.BACKDROP_IMAGE_ONLY
            } else {
                chosenBackdropStyle
            }
        val heroTrailerPlaying by viewModel.heroTrailerService.playing.collectAsStateWithLifecycle()
        // Otherwise the trailer plays inside a card, see CardTrailers
        val heroTrailerInCard by viewModel.heroTrailerService.inCard.collectAsStateWithLifecycle()
        Backdrop(
            drawerIsOpen = drawerState.isOpen,
            backdropStyle = backdropStyle,
            enableTopScrim = enableTopScrim,
            viewModel = viewModel,
            hideImage = heroTrailerPlaying && !heroTrailerInCard,
        )
        val heroTrailerArea = remember { HeroTrailerArea() }
        HeroTrailerBackdrop(
            heroTrailerService = viewModel.heroTrailerService,
            area = heroTrailerArea,
            muted = preferences.appPreferences.interfacePreferences.muteHeroTrailer,
        )
        val navDrawerListState = rememberLazyListState()
        CompositionLocalProvider(
            LocalHeroTrailerArea provides heroTrailerArea,
            LocalCardProgressStyle provides preferences.appPreferences.interfacePreferences.cardProgressStyle,
            LocalShowCardInfo provides !preferences.appPreferences.interfacePreferences.hideCardInfo,
            LocalHomeScreenStyle provides preferences.appPreferences.interfacePreferences.homeScreenStyle,
        ) {
            NavDisplay(
                backStack = navigationManager.backStack,
                onBack = { navigationManager.goBack() },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                entryProvider = { key ->
                    key as Destination
                    val contentKey = "${key}_${server?.id}_${user?.id}"
                    NavEntry(key, contentKey = contentKey) {
                        // Thorphin: the Alternative UI shows its rail next to the main settings too
                        val fullScreen =
                            key.fullScreen &&
                                !(
                                    homeScreenStyle.isAlternative &&
                                        key is Destination.Settings &&
                                        key.screen == PreferenceScreenOption.BASIC
                                )
                        if (fullScreen) {
                            DestinationContent(
                                destination = key,
                                preferences = preferences,
                                onClearBackdrop = viewModel::clearBackdrop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (user != null && server != null) {
                            NavDrawer(
                                destination = key,
                                preferences = preferences,
                                user = user,
                                server = server,
                                drawerState = drawerState,
                                navDrawerListState = navDrawerListState,
                                onClearBackdrop = viewModel::clearBackdrop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            ErrorMessage("Trying to go to $key without a user logged in", null)
                        }
                    }
                },
            )
        }
    }
}
