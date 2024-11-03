package com.readrops.app.timelime.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.more.preferences.PreferencesScreen
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.app.timelime.TimelineState
import com.readrops.app.util.components.BorderedIconButton
import com.readrops.app.util.components.FeedIcon
import com.readrops.app.util.theme.spacing
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.filters.MainFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineDrawer(
    state: TimelineState,
    screenModel: TimelineScreenModel,
    onClickDefaultItem: (MainFilter) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onFeedClick: (Feed) -> Unit,
    modifier: Modifier
) {
    var isSecondColumnVisible by remember { mutableStateOf(state.filters.folderId != 0 || state.filters.feedId != 0) }
    var selectedFolder by remember { mutableStateOf(screenModel.getSelectedFolder())}
    val navigator = LocalNavigator.currentOrThrow
    Row(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier
            .fillMaxHeight()
            .weight(0.4f)) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            BorderedIconButton (
                                onClick = { screenModel.closeDrawer() },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Drawer")
                            }
                        },
                        actions = {
                            BorderedIconButton (onClick = {
                                screenModel.closeDrawer()
                                navigator.push(PreferencesScreen()) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        })
                },
            ) { paddingValues ->

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    DrawerContent(
                        state = state,
                        selectedFolder = screenModel.getSelectedFolder(),
                        onClickDefaultItem = onClickDefaultItem,
                        onFolderClick = onFolderClick,
                        onFeedClick = onFeedClick,
                        onFolderExpand = {
                            selectedFolder = it
                            isSecondColumnVisible = true
                        }
                    )
                }

            }
        }
        VerticalDivider(
            color = Color.DarkGray,
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.4f)
        ) {
            if (isSecondColumnVisible) {
                Scaffold { paddingValues ->
                    val scrollState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(paddingValues)
                            .padding(horizontal = 0.dp, vertical = 12.dp)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        val feeds =
                            if (selectedFolder != null) state.foldersAndFeeds[selectedFolder] else null
                        if (feeds != null) {
                            LaunchedEffect(feeds, state.filters.feedId) {
                                feeds.indexOfFirst { it.id == state.filters.feedId }.let { index ->
                                    if (index != -1) {
                                        scrollState.requestScrollToItem(index)
                                    }
                                }
                            }
                            LazyColumn(state = scrollState) {
                                items(feeds.size) { feedIdx ->
                                    val feed = feeds[feedIdx]
                                    DrawerFeedItem(
                                        label = {
                                            Text(
                                                text = feed.name.orEmpty(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        icon = {
                                            FeedIcon(
                                                iconUrl = feed.iconUrl,
                                                name = feed.name.orEmpty()
                                            )
                                        },
                                        badge = { Text(feed.unreadCount.toString()) },
                                        selected = feed.id == state.filters.feedId,
                                        onClick = { onFeedClick(feed) },
                                        modifier = Modifier
                                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                                            .selectedItemBorder(feed.id == state.filters.feedId),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isSecondColumnVisible) {
            VerticalDivider(
                color = Color.DarkGray,
            )
        }
        Column(modifier = Modifier
            .fillMaxHeight()
            .weight(0.2f)
        ) {

        }
    }
}

@Composable
fun DrawerContent(
    state: TimelineState,
    selectedFolder: Folder?,
    onClickDefaultItem: (MainFilter) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onFeedClick: (Feed) -> Unit,
    onFolderExpand: (Folder) -> Unit,
) {

Column {
    DrawerDefaultItems(
        selectedItem = state.filters.mainFilter,
        unreadNewItemsCount = state.unreadNewItemsCount,
        onClick = { onClickDefaultItem(it) }
    )

    DrawerDivider()

    Column {
        for (folderEntry in state.foldersAndFeeds) {
            val folder = folderEntry.key

            if (folder != null) {
                DrawerFolderItem(
                    label = {
                        Text(
                            text = folder.name.orEmpty(),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    icon = {
                        Icon(
                            painterResource(id = R.drawable.ic_folder_grey),
                            contentDescription = null
                        )
                    },
                    badge = {
                        Text(folderEntry.value.sumOf { it.unreadCount }.toString())
                    },
                    selected = selectedFolder?.id == folder.id,
                    onClick = { onFolderClick(folder) },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .selectedItemBorder(selectedFolder?.id == folder.id),
                    onExpand = {onFolderExpand(folder)}
                )
            } else {
                val feeds = folderEntry.value

                for (feed in feeds) {
                    DrawerFeedItem(
                        label = {
                            Text(
                                text = feed.name.orEmpty(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            FeedIcon(
                                iconUrl = feed.iconUrl,
                                name = feed.name.orEmpty()
                            )
                        },
                        badge = { Text(feed.unreadCount.toString()) },
                        selected = feed.id == state.filters.feedId,
                        onClick = { onFeedClick(feed) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    }
}
}

@Composable
fun DrawerDefaultItems(
    selectedItem: MainFilter,
    unreadNewItemsCount: Int,
    onClick: (MainFilter) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(text = stringResource(R.string.articles)) },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_timeline),
                contentDescription = null
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent
        ),
        selected = selectedItem == MainFilter.ALL,
        onClick = { onClick(MainFilter.ALL) },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .selectedItemBorder(selectedItem == MainFilter.ALL),
        )

    NavigationDrawerItem(
        label = {
            Text(
                "${stringResource(id = R.string.new_articles)} (${
                    stringResource(
                        id = R.string.unread,
                        unreadNewItemsCount
                    )
                })"
            )
        },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_new),
                contentDescription = null
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent
        ),
        selected = selectedItem == MainFilter.NEW,
        onClick = { onClick(MainFilter.NEW) },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .selectedItemBorder(selectedItem == MainFilter.NEW),
    )

    NavigationDrawerItem(
        label = { Text(text = stringResource(R.string.favorites)) },
        icon = {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null
            )
        },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.Transparent,
            unselectedContainerColor = Color.Transparent
        ),
        selected = selectedItem == MainFilter.STARS,
        onClick = { onClick(MainFilter.STARS) },
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .selectedItemBorder(selectedItem == MainFilter.STARS),
    )
}

@Composable
fun DrawerDivider() {
    HorizontalDivider(
        thickness = 2.dp,
        modifier = Modifier.padding(
            vertical = MaterialTheme.spacing.drawerSpacing,
            horizontal = 28.dp // M3 guidelines
        )
    )
}

@Composable
fun Modifier.selectedItemBorder(isSelected: Boolean): Modifier {
    return if (isSelected) {
        this.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(4.dp)
        )
    } else {
        this
    }
}
