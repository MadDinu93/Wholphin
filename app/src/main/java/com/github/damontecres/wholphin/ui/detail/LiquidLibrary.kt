package com.github.damontecres.wholphin.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.ui.theme.LiquidColors
import com.github.damontecres.wholphin.ui.theme.LocalLiquidStyle

/**
 * The title of a Library page. The Modern and Squared styles show it large with a line of real counts under it, see
 * [com.github.damontecres.wholphin.ui.theme.LiquidStyleScope]; every other style keeps the plain title it always had.
 * Everything else on those pages, sorting, filtering and data, is the same for all styles.
 */
@Composable
fun LibraryTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    if (LocalLiquidStyle.current == null) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = modifier,
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = LiquidColors.TextPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = LiquidColors.TextSecondary,
                    modifier =
                        Modifier
                            .background(LiquidColors.Elevated, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}
