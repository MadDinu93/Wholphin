package com.github.damontecres.wholphin.ui.playback

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.github.damontecres.wholphin.services.HeroTrailerService
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.components.CircularProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TrailerVideoViewModel
    @Inject
    constructor(
        heroTrailerService: HeroTrailerService,
        val navigationManager: NavigationManager,
    ) : ViewModel() {
        // Not from the server, so the user's credentials are not sent along
        val player = heroTrailerService.createPlayer(fromServer = false)

        override fun onCleared() {
            player.release()
        }
    }

/**
 * Plays a trailer given as a link to the video itself full screen, going back once it ends or fails
 */
@OptIn(UnstableApi::class)
@Composable
fun TrailerVideoPage(
    url: String,
    modifier: Modifier = Modifier,
    viewModel: TrailerVideoViewModel = hiltViewModel(),
) {
    val player = viewModel.player
    var buffering by remember { mutableStateOf(true) }
    DisposableEffect(player, url) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                    if (playbackState == Player.STATE_ENDED) viewModel.navigationManager.goBack()
                }

                override fun onPlayerError(error: PlaybackException) {
                    viewModel.navigationManager.goBack()
                }
            }
        player.addListener(listener)
        if (player.currentMediaItem == null) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.pause()
        }
    }
    // Whole, neither cropped nor stretched
    val presentationState = rememberPresentationState(player)
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().background(Color.Black)) {
        PlayerSurface(
            player = player,
            modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp),
        )
        if (buffering) {
            CircularProgress(Modifier.size(48.dp).align(Alignment.Center))
        }
    }
}
