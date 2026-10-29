package com.github.damontecres.wholphin.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.theme.LiquidColors

/**
 * A settings category of the Alternative UI: a rounded row, filled amber while its settings are the ones shown
 */
@Composable
fun AlternativeCategoryItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocused() }
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (selected) LiquidColors.Accent else LiquidColors.Base,
                contentColor = if (selected) LiquidColors.OnAccent else LiquidColors.TextPrimary,
                focusedContainerColor = if (selected) LiquidColors.Accent else LiquidColors.Elevated,
                focusedContentColor = if (selected) LiquidColors.OnAccent else LiquidColors.Accent,
            ),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, LiquidColors.Accent), shape = shape),
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/** The server and the user of the Alternative UI settings, above the categories */
@Composable
fun AlternativeServerCard(
    serverName: String?,
    serverUrl: String?,
    userName: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .background(LiquidColors.Elevated, RoundedCornerShape(12.dp))
                .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(LiquidColors.Accent, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName ?: serverUrl.orEmpty(),
                color = LiquidColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(serverUrl?.takeIf { it != serverName }, userName).joinToString("  •  ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    color = LiquidColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
