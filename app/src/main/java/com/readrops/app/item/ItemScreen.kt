package com.readrops.app.item

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.item.view.ItemLinearLayout
import com.readrops.app.item.view.ItemWebView
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.app.util.FontPreference
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.BORDER_COLOR
import com.readrops.app.util.components.BORDER_WIDTH
import com.readrops.app.util.components.BorderedIconButton
import com.readrops.app.util.components.BorderedTextButton
import com.readrops.app.util.components.BorderedToggleIconButton
import com.readrops.app.util.components.CORNER_RADIUS
import com.readrops.app.util.components.CenteredProgressIndicator
import com.readrops.db.pojo.ItemWithFeed
import kotlinx.coroutines.delay
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class ItemScreen(
    private val itemId: Int,
    private val itemListIndex: Int?,
) : AndroidScreen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel =
            getScreenModel<ItemScreenModel>(parameters = { parametersOf(itemId) })
        val state by screenModel.state.collectAsStateWithLifecycle()
        val timelineScreenModel: TimelineScreenModel = koinInject()
        val timelineState by
            timelineScreenModel.timelineState.collectAsStateWithLifecycle()
        val timelineListItems = timelineState.itemState.collectAsLazyPagingItems()

        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground

        val snackbarHostState = remember { SnackbarHostState() }
        var refreshAndroidView by remember { mutableStateOf(false) }

        val bottomBarHeight = 64.dp

        var showTextFormatPopup by remember { mutableStateOf(false) }
        var readabilityState by remember { mutableStateOf(ReadabilityState.OFF) }
        var readableText by remember { mutableStateOf("") }

        LaunchedEffect(state.formatSettings) {
            if (!state.formatSettings.isDefault)
                refreshAndroidView = true
        }

        if (state.imageDialogUrl != null) {
            ItemImageDialog(
                onChoice = {
                    if (it == ItemImageChoice.SHARE) {
                        screenModel.shareImage(state.imageDialogUrl!!, context)
                    } else {
                        screenModel.downloadImage(state.imageDialogUrl!!, context)
                    }

                    screenModel.closeImageDialog()
                },
                onDismiss = { screenModel.closeImageDialog() }
            )
        }

        LaunchedEffect(state.fileDownloadedEvent) {
            if (state.fileDownloadedEvent) {
                snackbarHostState.showSnackbar(context.getString(R.string.downloaded_file))
            }
        }

        if (state.itemWithFeed != null) {
            val itemWithFeed = state.itemWithFeed!!
            val item = itemWithFeed.item

            val accentColor = primaryColor

            val colorScheme = when (state.theme) {
                "light" -> CustomTabsIntent.COLOR_SCHEME_LIGHT
                "dark" -> CustomTabsIntent.COLOR_SCHEME_DARK
                else -> CustomTabsIntent.COLOR_SCHEME_SYSTEM
            }

            fun openUrl(url: String) {
                if (state.openInExternalBrowser) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } else {
                    CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(
                            CustomTabColorSchemeParams
                                .Builder()
                                .setToolbarColor(accentColor.toArgb())
                                .build()
                        )
                        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                        .setUrlBarHidingEnabled(true)
                        .setColorScheme(colorScheme)
                        .build()
                        .launchUrl(context, url.toUri())
                }
            }

            var currentPage by remember {mutableStateOf(0)}
            var totalPages by remember {mutableStateOf(0)}
            val bottomBar = @Composable {
                ItemScreenBottomBar(
                    state = state.bottomBarState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(bottomBarHeight)
                        .offset {
                            IntOffset(0, 0)
                        },
                    pageInfo = Pair(currentPage, totalPages),
                    onShare = { screenModel.shareItem(item, context) },
                    onOpenUrl = { openUrl(item.link!!) },
                    onChangeReadState = {
                        if (itemListIndex != null &&
                            timelineListItems[itemListIndex]?.item?.id == itemId) {
                            timelineListItems[itemListIndex]!!.item.isRead = it
                        }
                        screenModel.setItemReadState(item.apply { isRead = it })
                    },
                    onChangeStarState = {
                        screenModel.setItemStarState(item.apply { isStarred = it })
                    },
                )
            }

            val getDeltaItem : (Int) -> ItemWithFeed? = { delta : Int ->
                var newItemWithFeed: ItemWithFeed? = null
                if (itemListIndex != null &&
                    timelineListItems[itemListIndex]!!.item.id == itemWithFeed.item.id) {
                    val newIndex = itemListIndex + delta
                    if ((delta < 0 && newIndex >= 0)
                        || (delta > 0 && newIndex < timelineListItems.itemCount)) {
                        newItemWithFeed = timelineListItems[newIndex]
                    }
                }
                newItemWithFeed
            }

            val replaceWithDeltaItem = { delta : Int ->
                val newItemWithFeed = getDeltaItem(delta)
                if (newItemWithFeed != null) {
                    val newIndex = itemListIndex!! + delta
                    timelineScreenModel.setItemRead(newItemWithFeed!!.item)
                    timelineScreenModel.setTimelineItemIndex(newIndex)
                    navigator.replace(
                        ItemScreen(
                            newItemWithFeed.item.id,
                            newIndex
                        )
                    )
                }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = bottomBar,
                topBar = {
                    TopAppBar(
                        title = {},
                        actions = {
                            BorderedToggleIconButton (
                                onCheckedChange = { checked ->
                                    if (checked && readableText.isEmpty()) {
                                        readabilityState = ReadabilityState.IN_PROGRESS
                                        screenModel.readableArticleText(itemWithFeed,
                                            { result ->
                                                readableText = result
                                                readabilityState = ReadabilityState.ON
                                                refreshAndroidView = true
                                            },
                                            { error ->
                                                readabilityState = ReadabilityState.OFF
                                            }
                                        )
                                    } else {
                                        readabilityState =
                                            if (checked) ReadabilityState.ON else ReadabilityState.OFF
                                        refreshAndroidView = true
                                    }
                                },
                                enabled = readabilityState != ReadabilityState.IN_PROGRESS,
                                checked = readabilityState == ReadabilityState.ON
                            ) {
                                var borderColor by remember { mutableStateOf(Color.Transparent) }
                                if (readabilityState != ReadabilityState.IN_PROGRESS) {
                                    borderColor = Color.Transparent
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_reader_mode),
                                    contentDescription = null
                                )
                                if (readabilityState == ReadabilityState.IN_PROGRESS) {
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            if (borderColor == BORDER_COLOR)
                                                borderColor = Color.Transparent
                                            else
                                                borderColor = BORDER_COLOR
                                            delay(500)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Bottom)
                                            .fillMaxSize()
                                            .border(
                                                width = BORDER_WIDTH,
                                                color = borderColor,
                                                shape = RoundedCornerShape(CORNER_RADIUS)
                                            )
                                    )
                                }
                            }
                            BorderedToggleIconButton(
                                checked = showTextFormatPopup,
                                onCheckedChange = { checked -> showTextFormatPopup = checked },
                                drawBottomTriangle = true
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TextFormat,
                                    contentDescription = "Text Formatting"
                                )
                            }
                            if (itemListIndex != null &&
                                timelineListItems[itemListIndex]?.item?.id == itemId) {
                                BorderedIconButton(
                                    enabled = (itemListIndex > 0),
                                    onClick = {
                                       replaceWithDeltaItem(-1)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous Article"
                                    )
                                }
                                BorderedIconButton(
                                    enabled = (itemListIndex < timelineListItems.itemCount - 1),
                                    onClick = {
                                        replaceWithDeltaItem(+1)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next Article"
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            BorderedIconButton (
                                onClick = { navigator.pop() },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    var itemWebView : ItemWebView = getKoin().get()
                    AndroidView(
                        factory = { context ->
                            ItemLinearLayout(
                                context = context,
                                onUrlClick = { url -> openUrl(url) },
                                onImageLongPress = { url -> screenModel.openImageDialog(url) },
                                onPageUpdate = { c: Int, t: Int ->
                                    currentPage = c
                                    totalPages = t
                                },
                                nextItem = {replaceWithDeltaItem(+1)},
                                previousItem = {replaceWithDeltaItem(-1)},
                                webView = itemWebView,
                                lrMarginPerc = state.formatSettings.LrMarginPerc,
                                tbMarginPerc = state.formatSettings.TbMarginPerc
                            )
                        },
                        update = { linearLayout ->

                            linearLayout.adjustMargins(state.formatSettings.TbMarginPerc,
                                state.formatSettings.LrMarginPerc)

                            if (refreshAndroidView) {
                                val webView = linearLayout.getChildAt(0) as ItemWebView

                                webView.loadText(
                                    itemWithFeed = itemWithFeed,
                                    accentColor = accentColor,
                                    backgroundColor = backgroundColor,
                                    onBackgroundColor = onBackgroundColor,
                                    justifyText = state.formatSettings.justifyText,
                                    textSizeMultiplier = state.formatSettings.textSizeMultiplier,
                                    lineSizeMultiplier = state.formatSettings.lineSizeMultiplier,
                                    font = state.formatSettings.font,
                                    readableText = if (readabilityState == ReadabilityState.ON) readableText else "",
                                    nextItem = getDeltaItem(+1),
                                    prevItem = getDeltaItem(-1),
                                    lrMarginPerc = state.formatSettings.LrMarginPerc,
                                )

                                refreshAndroidView = false
                            }
                        },
                        onRelease = { linearLayout: ItemLinearLayout ->
                            // Find and remove WebView if it exists in LinearLayout
                            val webView = linearLayout.getChildAt(0) as? ItemWebView
                            webView?.apply {
                                clearHistory()
                                loadUrl("about:blank")
                                onPause()
                                removeAllViews()
                            }
                            linearLayout.removeAllViews()
                        },
                        modifier = Modifier.matchParentSize()
                    )

                    if (showTextFormatPopup) {
                        val xOffset =
                            with(LocalDensity.current) {
                                -96.dp.toPx()
                            }
                        MoreOptionsPopup(
                            alignment = Alignment.TopEnd,
                            offset = IntOffset(xOffset.toInt(), 0),
                            onDismiss = { showTextFormatPopup = false },
                            onTextSizeSliderValueChange = { newValue ->
                                screenModel.setItemTextSizeMultiplier(newValue)
                            },
                            onLineSizeSliderValueChange = { newValue ->
                                screenModel.setItemLineSizeMultiplier(newValue)
                            },
                            textSizeSliderValue = state.formatSettings.textSizeMultiplier,
                            lineSizeSliderValue = state.formatSettings.lineSizeMultiplier,
                            isJustified = state.formatSettings.justifyText,
                            onJustifyToggle = { newValue ->
                                screenModel.setItemJustifyText(newValue)
                            },
                            lrMarginPerc = state.formatSettings.LrMarginPerc,
                            tbMarginPerc = state.formatSettings.TbMarginPerc,
                            onMarginsChange = { lr: Int, tb: Int->
                                screenModel.setItemMargins(lr, tb)
                            },
                            selectedFont = state.formatSettings.font,
                            onFontChange = { newFont ->
                                screenModel.setItemFont(newFont)
                            }
                        )
                    }
                }
            }
        } else {
            CenteredProgressIndicator()
        }
    }
}

@Composable
fun BorderedPopup(
    onDismiss: () -> Unit,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    offset: IntOffset = IntOffset(0, 0),
    content: @Composable () -> Unit,
) {

    Popup(
        onDismissRequest = onDismiss,
        alignment = alignment,
        offset = offset,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp)
                ),
            shape = RoundedCornerShape(4.dp),
        ) {
            content()
        }
    }
}

@Composable
fun MoreOptionsPopup(
    alignment: Alignment,
    offset: IntOffset,
    onDismiss: () -> Unit,
    onTextSizeSliderValueChange: (Float) -> Unit,
    onLineSizeSliderValueChange: (Float) -> Unit,
    textSizeSliderValue: Float,
    lineSizeSliderValue: Float,
    isJustified: Boolean,
    onJustifyToggle: (Boolean) -> Unit,
    lrMarginPerc: Int,
    tbMarginPerc: Int,
    onMarginsChange: (Int, Int) -> Unit,
    selectedFont: FontPreference,
    onFontChange: (FontPreference) -> Unit
) {
    val snapPoints = listOf(1f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2f)
    var expanded by remember { mutableStateOf(false) }

    val snapPointsMargins =
        listOf(
            0f,
            0.02f,
            0.04f,
            0.06f,
            0.08f,
            0.10f,
            0.12f,
            0.14f,
            0.16f,
            0.18f,
            0.20f,
        )

    val fontNames = mapOf(
        FontPreference.SERIF to "Serif",
        FontPreference.SANS_SERIF to "Sans serif",
        FontPreference.MONOSPACE to "Monospace",
        FontPreference.NEWSREADER to "Newsreader"
    )

    BorderedPopup(
        alignment = alignment,
        modifier = Modifier
        .padding(end = 8.dp),
        offset = offset,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .width(300.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BorderedIconButton (
                    onClick = {
                        val currentIndex = snapPoints.indexOfFirst { it == textSizeSliderValue }
                        if (currentIndex > 0) {
                            onTextSizeSliderValueChange(snapPoints[currentIndex - 1])
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.TextDecrease,
                        contentDescription = "Decrease"
                    )
                }

                Slider(
                    value = textSizeSliderValue,
                    onValueChange = { newValue ->
                        val value =
                            snapPoints.minByOrNull { kotlin.math.abs(it - newValue) }
                                ?: newValue
                        onTextSizeSliderValueChange(value)
                    },
                    valueRange = 1f..2f,
                    steps = 9,
                    modifier = Modifier.width(200.dp)
                )

                BorderedIconButton(
                    onClick = {
                        val currentIndex = snapPoints.indexOfFirst { it == textSizeSliderValue }
                        if (currentIndex < snapPoints.size - 1) {
                            onTextSizeSliderValueChange(snapPoints[currentIndex + 1])
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.TextIncrease,
                        contentDescription = "Increase"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BorderedIconButton (
                    onClick = {
                        val currentIndex = snapPoints.indexOfFirst { it == lineSizeSliderValue }
                        if (currentIndex > 0) {
                            onLineSizeSliderValueChange(snapPoints[currentIndex - 1])
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.DensitySmall,
                        contentDescription = "Decrease"
                    )
                }

                Slider(
                    value = lineSizeSliderValue,
                    onValueChange = { newValue ->
                        val value =
                            snapPoints.minByOrNull { kotlin.math.abs(it - newValue) }
                                ?: newValue
                        onLineSizeSliderValueChange(value)
                    },
                    valueRange = 1f..2f,
                    steps = 9,
                    modifier = Modifier.width(200.dp)
                )

                BorderedIconButton(
                    onClick = {
                        val currentIndex = snapPoints.indexOfFirst { it == lineSizeSliderValue }
                        if (currentIndex < snapPoints.size - 1) {
                            onLineSizeSliderValueChange(snapPoints[currentIndex + 1])
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.DensityLarge,
                        contentDescription = "Increase"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Justify Text",
                    modifier = Modifier.padding(12.dp)
                )
                Switch(
                    checked = isJustified,
                    onCheckedChange = onJustifyToggle
                )
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BorderedIconButton (
                    onClick = {
                        val currentIndex = snapPointsMargins.indexOfFirst { it == lrMarginPerc/100f }
                        if (currentIndex > 0) {
                            onMarginsChange(
                                (snapPointsMargins[currentIndex - 1] * 100).toInt(),
                                tbMarginPerc)
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Expand,
                        contentDescription = "Decrease",
                        modifier = Modifier.rotate(90f)
                    )
                }

                Slider(
                    value = lrMarginPerc / 100f,
                    onValueChange = { newValue ->
                        val value =
                            snapPointsMargins.minByOrNull { kotlin.math.abs(it - newValue) }
                                ?: newValue
                        onMarginsChange((value * 100).toInt(), tbMarginPerc)
                    },
                    valueRange = 0f..0.20f,
                    steps = 9,
                    modifier = Modifier.width(200.dp)
                )

                BorderedIconButton(
                    onClick = {
                        val currentIndex = snapPointsMargins.indexOfFirst { it == lrMarginPerc/100f }
                        if (currentIndex < snapPointsMargins.size - 1) {
                            onMarginsChange(
                                (snapPointsMargins[currentIndex + 1] * 100).toInt(),
                                tbMarginPerc
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Compress,
                        contentDescription = "Increase",
                        modifier = Modifier.rotate(90f)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BorderedIconButton (
                    onClick = {
                        val currentIndex = snapPointsMargins.indexOfFirst { it == tbMarginPerc/100f }
                        if (currentIndex > 0) {
                            onMarginsChange(
                                lrMarginPerc, (snapPointsMargins[currentIndex - 1] * 100).toInt())
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.Expand,
                        contentDescription = "Decrease",
                    )
                }

                Slider(
                    value = tbMarginPerc / 100f,
                    onValueChange = { newValue ->
                        val value =
                            snapPointsMargins.minByOrNull { kotlin.math.abs(it - newValue) }
                                ?: newValue
                        onMarginsChange(lrMarginPerc, (value * 100).toInt())
                    },
                    valueRange = 0f..0.20f,
                    steps = 9,
                    modifier = Modifier.width(200.dp)
                )

                BorderedIconButton(
                    onClick = {
                        val currentIndex = snapPointsMargins.indexOfFirst { it == tbMarginPerc/100f }
                        if (currentIndex < snapPointsMargins.size - 1) {
                            onMarginsChange(
                                lrMarginPerc,
                                (snapPointsMargins[currentIndex + 1] * 100).toInt()
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Compress,
                        contentDescription = "Increase",
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Font",
                    modifier = Modifier.padding(12.dp)
                )
                Box {
                    BorderedTextButton (
                        onClick = { expanded = true },
                        modifier = Modifier.width(150.dp)
                    ) {
                        Text(fontNames[selectedFont]!!)
                    }
                    if (expanded) {
                        Surface(
                            modifier = Modifier
                                .width(150.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ),
                            shape = RoundedCornerShape(4.dp),
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column {
                                fontNames.forEach { (pref, name) ->
                                    TextButton(
                                        onClick = {
                                            onFontChange(pref)
                                            expanded = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
