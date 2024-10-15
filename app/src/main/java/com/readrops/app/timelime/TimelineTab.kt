package com.readrops.app.timelime

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.readrops.app.R
import com.readrops.app.item.ItemScreen
import com.readrops.app.timelime.drawer.TimelineDrawer
import com.readrops.app.util.ErrorMessage
import com.readrops.app.util.components.BorderedIconButton
import com.readrops.app.util.components.BorderedToggleIconButton
import com.readrops.app.util.components.CenteredProgressIndicator
import com.readrops.app.util.components.Placeholder
import com.readrops.app.util.components.RefreshScreen
import com.readrops.app.util.components.dialog.TwoChoicesDialog
import com.readrops.app.util.theme.spacing
import com.readrops.db.filters.MainFilter
import com.readrops.db.filters.OrderField
import com.readrops.db.filters.OrderType
import com.readrops.db.filters.SubFilter
import com.readrops.db.pojo.ItemWithFeed
import kotlinx.coroutines.flow.filter
import kotlin.math.abs


object TimelineTab : Tab {
    private fun readResolve(): Any = TimelineTab

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = stringResource(id = R.string.timeline),
        )

    @SuppressLint("InlinedApi")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current

        val screenModel = getScreenModel<TimelineScreenModel>()
        val state by screenModel.timelineState.collectAsStateWithLifecycle()
        val preferences = state.preferences
        val items = state.itemState.collectAsLazyPagingItems()

        val lazyListState = rememberLazyListState()
        var lazyRowHeight by remember { mutableStateOf(0.dp) }
        val snackbarHostState = remember { SnackbarHostState() }
        val topAppBarState = rememberTopAppBarState()
        val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var totalPages by remember { mutableStateOf(0) }

        val launcher =
            rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
                screenModel.disableDisplayNotificationsPermission()
            }

        val nextListPage = {
            if (state.currentPage < totalPages - 1)
                screenModel.setCurrentTimelinePage(state.currentPage + 1)
        }
        val prevListPage = {
            if (state.currentPage != 0)
                screenModel.setCurrentTimelinePage(state.currentPage - 1)
        }

        LaunchedEffect(preferences.displayNotificationsPermission) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                && preferences.displayNotificationsPermission
            ) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val drawerState = rememberDrawerState(
            initialValue = DrawerValue.Closed,
            confirmStateChange = {
                if (it == DrawerValue.Closed) {
                    screenModel.closeDrawer()
                } else {
                    screenModel.openDrawer()
                }

                true
            }
        )

        BackHandler(
            enabled = state.isDrawerOpen,
            onBack = { screenModel.closeDrawer() }
        )

        LaunchedEffect(state.isDrawerOpen) {
            if (state.isDrawerOpen) {
                drawerState.open()
            } else {
                drawerState.close()
            }
        }

        LaunchedEffect(state.localSyncErrors) {
            if (state.localSyncErrors != null) {
                val action = snackbarHostState.showSnackbar(
                    message = context.resources.getQuantityString(
                        R.plurals.error_occurred,
                        state.localSyncErrors!!.size
                    ),
                    actionLabel = context.getString(R.string.details),
                    duration = SnackbarDuration.Short
                )

                if (action == SnackbarResult.ActionPerformed) {
                    screenModel.openDialog(DialogState.ErrorList(state.localSyncErrors!!))
                } else {
                    // remove errors from state
                    screenModel.closeDialog(DialogState.ErrorList(state.localSyncErrors!!))
                }
            }
        }

        LaunchedEffect(state.syncError) {
            if (state.syncError != null) {
                snackbarHostState.showSnackbar(ErrorMessage.get(state.syncError!!, context))
                screenModel.resetSyncError()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = when (state.filters.mainFilter) {
                                        MainFilter.STARS -> stringResource(R.string.favorites)
                                        MainFilter.ALL -> stringResource(R.string.articles)
                                        MainFilter.NEW -> stringResource(R.string.new_articles)
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (state.showSubtitle) {
                                    Text(
                                        text = when (state.filters.subFilter) {
                                            SubFilter.FEED -> state.filterFeedName
                                            SubFilter.FOLDER -> state.filterFolderName
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            BorderedIconButton (
                                onClick = { screenModel.openDrawer() },
                                modifier = Modifier,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null
                                )
                            }
                        },
                        actions = {
                            /*
                            IconButton(
                                onClick = { screenModel.openDialog(DialogState.FilterSheet) }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_filter_list),
                                    contentDescription = null
                                )
                            }
                             */

                            BorderedToggleIconButton (
                                checked = preferences.showReadItems,
                                onCheckedChange = { checked ->
                                    screenModel.setShowReadItemsState(
                                        checked
                                    )
                                }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_list_alt_check
                                    ),
                                    contentDescription = null,
                                )
                            }

                            BorderedIconButton (
                                onClick = { screenModel.refreshTimeline(context) }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sync),
                                    contentDescription = null
                                )
                            }
                        },
                        scrollBehavior = topAppBarScrollBehavior
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { paddingValues ->
                var offsetX by remember { mutableStateOf(0f) }
                val velocityTracker = remember { VelocityTracker() }
                val SWIPE_THRESHOLD = 30
                val SWIPE_VELOCITY_THRESHOLD = 30

                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    velocityTracker.resetTracking()
                                    offsetX = 0f
                                },
                                onDragEnd = {
                                    val velocity = velocityTracker.calculateVelocity()
                                    if (abs(offsetX) > SWIPE_THRESHOLD && abs(velocity.x) > SWIPE_VELOCITY_THRESHOLD) {
                                        if (offsetX > 0) {
                                            prevListPage()
                                        } else {
                                            nextListPage()
                                        }
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount
                                    velocityTracker.addPosition(
                                        change.uptimeMillis,
                                        change.position
                                    )
                                }
                            )
                        }
                ) {
                    Column {
                        HorizontalDivider(
                            color = Color.DarkGray,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.largeSpacing
                            )
                        )
                    when {
                        state.displayRefreshScreen -> RefreshScreen(
                            currentFeed = state.currentFeed,
                            feedCount = state.feedCount,
                            feedMax = state.feedMax
                        )

                        items.isLoading() -> {
                            CenteredProgressIndicator()
                        }

                        items.isError() -> {
                            Placeholder(
                                text = stringResource(R.string.error_occured),
                                painter = painterResource(id = R.drawable.ic_error)
                            )
                        }

                        else -> {
                            MarkItemsRead(
                                lazyListState = lazyListState,
                                items = items,
                                markReadOnScroll = preferences.markReadOnScroll,
                                screenModel = screenModel
                            )

                            val density = LocalDensity.current
                            val minTimelinePadding = 15.dp
                            val timelineItemHeight = 68.dp + 2.dp + minTimelinePadding * 2
                            var actualTimelinePadding = minTimelinePadding

                            LaunchedEffect(state.currentPage) {
                                lazyListState.scrollToItem(state.currentPage)
                            }
                            Column (modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                ) {
                                    LazyRow(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .onSizeChanged { size ->
                                                if (lazyRowHeight == 0.dp) {
                                                    lazyRowHeight =
                                                        with(density) { size.height.toDp() }
                                                }
                                            }
                                            .padding(top = actualTimelinePadding),
                                        verticalAlignment = Alignment.Top,
                                        userScrollEnabled = false,
                                        state = lazyListState,
                                        contentPadding = PaddingValues(
                                            horizontal = 50.dp,
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(50.dp)
                                    ) {
                                        val itemsPerColumn = if (lazyRowHeight > 0.dp) {
                                            (lazyRowHeight / timelineItemHeight).toInt()
                                        } else {
                                            0 // Default value when height is not yet measured
                                        }
                                        if (itemsPerColumn > 0) {
                                            actualTimelinePadding =
                                                minTimelinePadding + (lazyRowHeight - (timelineItemHeight * itemsPerColumn)) / (2 * itemsPerColumn)
                                            totalPages =
                                                (items.itemCount + itemsPerColumn - 1) / itemsPerColumn
                                        }
                                        items(
                                            count = totalPages,
                                            key = { it } // Use the index as the key
                                        ) { pageIndex ->
                                            Column(
                                                modifier = Modifier.fillParentMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(actualTimelinePadding)
                                            ) {
                                                repeat(itemsPerColumn) { columnIndex ->
                                                    val itemIndex = pageIndex * itemsPerColumn + columnIndex
                                                    if (itemIndex < items.itemCount) {
                                                        val itemWithFeed = items[itemIndex]
                                                        if (itemWithFeed != null) {
                                                            TimelineItem(
                                                                itemWithFeed = itemWithFeed,
                                                                onClick = {
                                                                    screenModel.setItemRead(
                                                                        itemWithFeed.item
                                                                    )
                                                                    navigator.push(
                                                                        ItemScreen(
                                                                            itemId = itemWithFeed.item.id,
                                                                        )
                                                                    )
                                                                },
                                                                onFavorite = {
                                                                    screenModel.updateStarState(
                                                                        itemWithFeed.item
                                                                    )
                                                                },
                                                                onShare = {
                                                                    screenModel.shareItem(
                                                                        itemWithFeed.item,
                                                                        context
                                                                    )
                                                                },
                                                                onSetReadState = {
                                                                    screenModel.updateItemReadState(
                                                                        itemWithFeed.item
                                                                    )
                                                                },
                                                                size = preferences.itemSize,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )

                                                            if (columnIndex != itemsPerColumn - 1) {
                                                                HorizontalDivider(
                                                                    modifier = Modifier.padding(
                                                                        horizontal = MaterialTheme.spacing.shortSpacing
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Column {
                                    HorizontalDivider(
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(
                                            horizontal = MaterialTheme.spacing.largeSpacing
                                        )
                                    )
                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 5.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val buttonWidth = 50.dp

                                        BorderedIconButton (
                                            modifier = Modifier.width(buttonWidth),
                                            onClick = {
                                                screenModel.setCurrentTimelinePage(0)
                                            },
                                            enabled = state.currentPage > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardDoubleArrowLeft,
                                                contentDescription = "First page"
                                            )
                                        }
                                        BorderedIconButton (
                                            modifier = Modifier.width(buttonWidth),
                                            onClick = {
                                                prevListPage()
                                            },
                                            enabled = state.currentPage > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardArrowLeft,
                                                contentDescription = "Previous page"
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            val currentPage = if (totalPages != 0) state.currentPage + 1 else 0
                                            Text(
                                                "${currentPage}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(" / $totalPages")
                                        }
                                        BorderedIconButton(
                                            modifier = Modifier.width(buttonWidth),
                                            onClick = {
                                                nextListPage()
                                            },
                                            enabled = if (totalPages != 0) state.currentPage != (totalPages - 1) else false
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowRight,
                                                contentDescription = "Next page"
                                            )
                                        }
                                        BorderedIconButton (
                                            modifier = Modifier.width(buttonWidth),
                                            onClick = {
                                                screenModel.setCurrentTimelinePage(totalPages - 1)
                                            },
                                            enabled = if (totalPages != 0) state.currentPage != (totalPages - 1) else false
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.KeyboardDoubleArrowRight,
                                                contentDescription = "Last page"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                        }
                }
            }

            if (state.isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInteropFilter {
                            screenModel.closeDrawer()
                            true
                        }
                )

                TimelineDrawer(
                    state = state,
                    screenModel = screenModel,
                    onClickDefaultItem = {
                        screenModel.updateDrawerDefaultItem(it)
                        screenModel.closeDrawer()
                    },
                    onFolderClick = {
                        screenModel.updateDrawerFolderSelection(it)
                        screenModel.closeDrawer()
                    },
                    onFeedClick = {
                        screenModel.updateDrawerFeedSelection(it)
                        screenModel.closeDrawer()
                    },
                    modifier = Modifier.matchParentSize(),
                )
            }

            when (val dialog = state.dialog) {
                is DialogState.ConfirmDialog -> {
                    TwoChoicesDialog(
                        title = stringResource(R.string.mark_all_articles_read),
                        text = stringResource(R.string.mark_all_articles_read_question),
                        icon = painterResource(id = R.drawable.ic_rss_feed_grey),
                        confirmText = stringResource(id = R.string.validate),
                        dismissText = stringResource(id = R.string.cancel),
                        onDismiss = { screenModel.closeDialog() },
                        onConfirm = {
                            screenModel.closeDialog()
                            screenModel.setAllItemsRead()
                        }
                    )
                }

                is DialogState.FilterSheet -> {
                    FilterBottomSheet(
                        filters = state.filters,
                        onSetShowReadItems = {
                            screenModel.setShowReadItemsState(!state.filters.showReadItems)
                        },
                        onSetOrderField = {
                            screenModel.setOrderFieldState(
                                if (state.filters.orderField == OrderField.ID) {
                                    OrderField.DATE
                                } else {
                                    OrderField.ID
                                }
                            )
                        },
                        onSetOrderType = {
                            screenModel.setOrderTypeState(
                                if (state.filters.orderType == OrderType.DESC) {
                                    OrderType.ASC
                                } else {
                                    OrderType.DESC
                                }
                            )
                        },
                        onDismiss = { screenModel.closeDialog() }
                    )
                }

                is DialogState.ErrorList -> {
                    ErrorListDialog(
                        errorResult = dialog.errorResult,
                        onDismiss = { screenModel.closeDialog(dialog) }
                    )
                }

                null -> {}
            }
        }
    }

    @Composable
    private fun MarkItemsRead(
        lazyListState: LazyListState,
        items: LazyPagingItems<ItemWithFeed>,
        markReadOnScroll: Boolean,
        screenModel: TimelineScreenModel
    ) {
        val lastFirstVisibleItemIndex by screenModel.listIndexState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            snapshotFlow { lazyListState.firstVisibleItemIndex }
                .filter {
                    if (it < lastFirstVisibleItemIndex) {
                        screenModel.updateLastFirstVisibleItemIndex(it)
                    }

                    it > lastFirstVisibleItemIndex
                }
                .collect { newLastFirstVisibleItemIndex ->
                    if (newLastFirstVisibleItemIndex - lastFirstVisibleItemIndex > 1) {
                        val difference = newLastFirstVisibleItemIndex - lastFirstVisibleItemIndex

                        for (subCount in 0 until difference) {
                            val item = items[lastFirstVisibleItemIndex + subCount]?.item

                            if (item != null && !item.isRead && markReadOnScroll) {
                                screenModel.setItemRead(item)
                            }
                        }
                    } else {
                        val item = items[lastFirstVisibleItemIndex]?.item

                        if (item != null && !item.isRead && markReadOnScroll) {
                            screenModel.setItemRead(item)
                        }
                    }

                    screenModel.updateLastFirstVisibleItemIndex(newLastFirstVisibleItemIndex)
                }
        }
    }
}


fun <T : Any> LazyPagingItems<T>.isLoading(): Boolean {
    return loadState.refresh is LoadState.Loading && itemCount == 0
}

fun <T : Any> LazyPagingItems<T>.isError(): Boolean {
    return loadState.append is LoadState.Error //|| loadState.refresh is LoadState.Error
}