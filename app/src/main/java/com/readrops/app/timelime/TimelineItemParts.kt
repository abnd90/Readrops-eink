package com.readrops.app.timelime

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.readrops.app.R
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.util.DateUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun RegularTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (itemWithFeed.item.isRead) Color.Gray else Color.Black;
    TimelineItemContainer(
        isRead = itemWithFeed.item.isRead,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)
        ) {
            TimelineItemHeader(
                textColor = textColor,
                feedName = itemWithFeed.feedName,
                feedIconUrl = itemWithFeed.feedIconUrl,
                feedColor = itemWithFeed.color,
                folderName = itemWithFeed.folder?.name,
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                isStarred = itemWithFeed.item.isStarred,
                onFavorite = onFavorite,
                onShare = onShare
            )

            ShortSpacer()

            TimelineItemTitle(title = itemWithFeed.item.title!!)

            ShortSpacer()

            TimelineItemBadge(
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                textColor = textColor,
            )
        }
    }
}

@Composable
fun CompactTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.background
    val textColor = if (itemWithFeed.item.isRead) Color(0xff666666) else Color.Black;
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        color = Color.Transparent,
        modifier = modifier
            .drawBehind {
                // if some alpha is applied to the card, the swipe to dismiss background appears behind it
                // so we draw a rect with the current screen background color behind the card but in front of the dismiss background
                drawRect(containerColor)
            }
            .padding(top=1.dp)
            .clickable( onClick = onClick, interactionSource = interactionSource, indication = null)
    ) {
        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.shortSpacing,
                end = MaterialTheme.spacing.shortSpacing,
                //top = MaterialTheme.spacing.shortSpacing
            )
        ) {
            TimelineItemHeader(
                textColor = textColor,
                feedName = itemWithFeed.feedName,
                feedIconUrl = itemWithFeed.feedIconUrl,
                feedColor = itemWithFeed.color,
                folderName = itemWithFeed.folder?.name,
                onFavorite = onFavorite,
                onShare = onShare,
                date = itemWithFeed.item.pubDate!!,
                duration = itemWithFeed.item.readTime,
                isStarred = itemWithFeed.item.isStarred,
                displayActions = false
            )

            ShortSpacer()

            TimelineItemTitle(title = itemWithFeed.item.title!!, textColor = textColor)
        }
    }
}

@Composable
fun LargeTimelineItem(
    itemWithFeed: ItemWithFeed,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (itemWithFeed.item.cleanDescription == null && !itemWithFeed.item.hasImage) {
        RegularTimelineItem(
            itemWithFeed = itemWithFeed,
            onClick = onClick,
            onFavorite = onFavorite,
            onShare = onShare,
            modifier = modifier
        )
    } else {
        TimelineItemContainer(
            isRead = itemWithFeed.item.isRead,
            onClick = onClick,
            modifier = modifier
        ) {
            Column {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.mediumSpacing)
                ) {
                    TimelineItemHeader(
                        textColor = Color.Black,
                        feedName = itemWithFeed.feedName,
                        feedIconUrl = itemWithFeed.feedIconUrl,
                        feedColor = itemWithFeed.color,
                        folderName = itemWithFeed.folder?.name,
                        date = itemWithFeed.item.pubDate!!,
                        duration = itemWithFeed.item.readTime,
                        isStarred = itemWithFeed.item.isStarred,
                        onFavorite = onFavorite,
                        onShare = onShare
                    )

                    ShortSpacer()

                    TimelineItemBadge(
                        date = itemWithFeed.item.pubDate!!,
                        duration = itemWithFeed.item.readTime,
                        textColor = Color.Black
                    )

                    ShortSpacer()

                    TimelineItemTitle(title = itemWithFeed.item.title!!)

                    if (itemWithFeed.item.cleanDescription != null) {
                        ShortSpacer()

                        Text(
                            text = itemWithFeed.item.cleanDescription!!,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (itemWithFeed.item.hasImage) {
                    AsyncImage(
                        model = if (!LocalInspectionMode.current) {
                            itemWithFeed.item.imageLink
                        } else {
                            ImageRequest.Builder(LocalContext.current)
                                .data(R.drawable.ic_broken_image)
                                .build()
                        },
                        contentDescription = itemWithFeed.item.title!!,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

}

@Composable
fun TimelineItemContainer(
    isRead: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = MaterialTheme.spacing.shortSpacing),
    content: @Composable () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.background

    Card(
        modifier = modifier
            .padding(padding)
            .fillMaxWidth()
            .drawBehind {
                // if some alpha is applied to the card, the swipe to dismiss background appears behind it
                // so we draw a rect with the current screen background color behind the card but in front of the dismiss background
                drawRoundRect(
                    color = containerColor,
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .alpha(if (isRead) readAlpha else 1f)
            .clickable { onClick() }
    ) {
        content()
    }
}

@Composable
fun TimelineItemHeader(
    textColor: Color,
    feedName: String,
    feedIconUrl: String?,
    feedColor: Int,
    folderName: String?,
    date: LocalDateTime,
    duration: Double,
    isStarred: Boolean,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    displayActions: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            /*
            FeedIcon(
                iconUrl = feedIconUrl,
                name = feedName
            )
             */

            Column {
                Text(
                    text = feedName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    /*
                    color = if (feedColor != 0) {
                        Color(feedColor)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                     */
                )

                if (!folderName.isNullOrEmpty()) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }

        ShortSpacer()

        if (displayActions) {
            Row {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    IconButton(
                        onClick = onFavorite
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isStarred) R.drawable.ic_star
                                else R.drawable.ic_star_outline
                            ),
                            contentDescription = null,
                        )
                    }
                }

                ShortSpacer()

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    IconButton(
                        onClick = onShare
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            TimelineItemBadge(
                date = date,
                duration = duration,
                textColor = textColor
            )
        }
    }
}


@Composable
fun TimelineItemTitle(
    title: String,
    textColor : Color = Color.Black
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.Bold,
        color = textColor,
    )
}

@Composable
fun TimelineItemBadge(
    date: LocalDateTime,
    duration: Double,
    textColor: Color
) {


    Surface(
        color = Color.Transparent
        //color = if (color != 0) Color(color) else MaterialTheme.colorScheme.primary,
        //shape = RoundedCornerShape(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.shortSpacing,
                vertical = MaterialTheme.spacing.veryShortSpacing
            )
        ) {
            Text(
                text = DateUtils.formattedDateByLocal(date),
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )

            Text(
                text = stringResource(id = R.string.interpoint),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.veryShortSpacing),
                color = textColor
            )

            Text(
                text = if (duration > 1) {
                    stringResource(id = R.string.read_time, duration.roundToInt())
                } else {
                    stringResource(id = R.string.read_time_lower_than_1)
                },
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}

private class BorderNode(private val interactionSource: InteractionSource) :
    Modifier.Node(), DrawModifierNode {

    var currentPressPosition: Offset = Offset.Zero
    val animatedScalePercent = Animatable(1f)

    private val borderColor = Color.Black
    private val borderWidth = 2.dp
    private val cornerRadius = 4.dp

    private var isPressed by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collectLatest { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {isPressed = true}
                    is PressInteraction.Release, is PressInteraction.Cancel -> {isPressed = false}
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        if (isPressed) {
            drawRoundRect(
                color = borderColor,
                style = Stroke(width = borderWidth.toPx()),
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
}

object BorderIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return BorderNode(interactionSource)
    }

    override fun equals(other: Any?): Boolean = other === BorderIndication
    override fun hashCode() = 100
}