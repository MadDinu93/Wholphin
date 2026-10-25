package com.github.damontecres.wholphin.ui.cards

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.tryRequestFocus

@Composable
fun <T> ItemRow(
    title: String,
    items: List<T?>,
    onClickItem: (Int, T) -> Unit,
    onLongClickItem: (Int, T) -> Unit,
    cardContent: @Composable (
        index: Int,
        item: T?,
        modifier: Modifier,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
    ) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    showViewMore: Boolean = false,
    viewMoreCardContent: @Composable (Modifier) -> Unit = {},
) {
    val state = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    val focusRequester = remember { FocusRequester() }
    var position by rememberInt()

    val currentOnClickItem by rememberUpdatedState(onClickItem)
    val currentOnLongClickItem by rememberUpdatedState(onLongClickItem)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier.focusProperties {
                onEnter = {
                    focusRequester.tryRequestFocus()
                }
            },
    ) {
        ItemRowTitle(title)

        CompositionLocalProvider(LocalItemRowListState provides state) {
            LazyRow(
                state = state,
                horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusGroup()
                        .focusRestorer(firstFocus)
                        .focusRequester(focusRequester),
            ) {
                itemsIndexed(items) { index, item ->
                    val cardModifier =
                        remember(index, position) {
                            if (index == position) {
                                Modifier.focusRequester(firstFocus)
                            } else {
                                Modifier
                            }
                        }

                    val onClick =
                        remember(index, item) {
                            {
                                position = index
                                if (item != null) currentOnClickItem(index, item)
                            }
                        }

                    val onLongClick =
                        remember(index, item) {
                            {
                                position = index
                                if (item != null) currentOnLongClickItem(index, item)
                            }
                        }

                    cardContent.invoke(
                        index,
                        item,
                        cardModifier,
                        onClick,
                        onLongClick,
                    )
                }
                if (showViewMore) {
                    item {
                        val cardModifier =
                            remember(items.size, position) {
                                if (position == items.size) {
                                    Modifier.focusRequester(firstFocus)
                                } else {
                                    Modifier
                                }
                            }
                        viewMoreCardContent.invoke(cardModifier)
                    }
                }
            }
        }
    }
}

/**
 * The scroll state of the [ItemRow] a card is in
 */
val LocalItemRowListState = compositionLocalOf<LazyListState?> { null }

/**
 * While the card at [index] of the row grows, scrolls the row so the card grows on both sides, or only away from the
 * screen edge it is at, so the whole card stays on screen
 */
@Composable
fun KeepGrowingCardInView(
    state: LazyListState,
    index: Int,
) {
    val edgePx = with(LocalDensity.current) { 16.dp.toPx() }
    LaunchedEffect(state, index) {
        var lastSize = -1
        snapshotFlow { state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size }
            .collect { size ->
                if (size == null) return@collect
                val delta = if (lastSize < 0) 0 else size - lastSize
                lastSize = size
                if (delta <= 0) return@collect
                val layoutInfo = state.layoutInfo
                val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return@collect
                // Room for the card being scaled up while focused
                val margin = edgePx + item.size * .05f
                // Scrolling by more would push the start of the card off screen
                val most = item.offset - (layoutInfo.viewportStartOffset + margin)
                // Scrolling by less would leave the end of the card off screen
                val least = item.offset + item.size - (layoutInfo.viewportEndOffset - margin)
                val scroll = (delta / 2f).coerceAtMost(most).coerceAtLeast(least)
                if (scroll != 0f) state.dispatchRawDelta(scroll)
            }
    }
}

@Composable
@NonRestartableComposable
fun ItemRowTitle(
    title: String,
    modifier: Modifier = Modifier,
) = Text(
    text = title,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = modifier.padding(start = 8.dp),
)
