package com.readrops.app.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.readrops.app.R
import com.readrops.app.util.components.BorderedIconButton
import com.readrops.app.util.components.BorderedToggleIconButton
import com.readrops.app.util.theme.spacing

data class BottomBarState(
    val isRead: Boolean = false,
    val isStarred: Boolean = false
)

enum class ReadabilityState {
    OFF, ON, IN_PROGRESS
}

@Composable
fun ItemScreenBottomBar(
    state: BottomBarState,
    pageInfo: Pair<Int, Int>,
    onShare: () -> Unit,
    onOpenUrl: () -> Unit,
    onChangeReadState: (Boolean) -> Unit,
    onChangeStarState: (Boolean) -> Unit,
    readabilityState: ReadabilityState,
    onReadability: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    /*
    val tint = if (FeedColors.isColorDark(accentColor.toArgb()))
        Color.White
    else
        Color.Black
     */
    val tint = Color.Black
    val tintOff = Color(0xff666666)

    Surface(
        //color = accentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.padding(MaterialTheme.spacing.shortSpacing)
        ) {
            BorderedIconButton (
                onClick = { onChangeReadState(!state.isRead) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isRead)
                            R.drawable.ic_remove_done
                        else R.drawable.ic_done_all
                    ),
                    tint = tint,
                    contentDescription = null
                )
            }

            BorderedIconButton (
                onClick = { onChangeStarState(!state.isStarred) }
            ) {
                Icon(
                    painter = painterResource(
                        id = if (state.isStarred)
                            R.drawable.ic_star
                        else R.drawable.ic_star_outline
                    ),
                    tint = tint,
                    contentDescription = null
                )
            }

            BorderedIconButton (
                onClick = onShare
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    tint = tint,
                    contentDescription = null
                )
            }

            BorderedIconButton (
                onClick = onOpenUrl
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_open_in_browser),
                    tint = tint,
                    contentDescription = null
                )
            }

            BorderedToggleIconButton (
                onCheckedChange = {
                    checked ->
                        onReadability(checked)
                },
                enabled = readabilityState != ReadabilityState.IN_PROGRESS,
                checked = readabilityState == ReadabilityState.ON
            ) {
                when (readabilityState) {
                    ReadabilityState.IN_PROGRESS -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_reader_mode),
                            tint = tint,
                            contentDescription = null
                        )
                    }
                }
            }

            Text("${pageInfo.first + 1} / ${pageInfo.second}", modifier = Modifier.align(Alignment.CenterVertically))
        }
    }
}
