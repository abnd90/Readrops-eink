package com.readrops.app.item

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.readrops.app.R
import com.readrops.app.item.view.ItemLinearLayout
import com.readrops.app.item.view.ItemWebView
import com.readrops.app.timelime.TimelineScreenModel
import com.readrops.app.util.Preferences
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.BorderedIconButton
import com.readrops.app.util.components.BorderedToggleIconButton
import com.readrops.app.util.components.CenteredProgressIndicator
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

class ItemScreen(
    private val itemId: Int,
) : AndroidScreen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val preferences = koinInject<Preferences>()

        val context = LocalContext.current
        val density = LocalDensity.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel =
            getScreenModel<ItemScreenModel>(parameters = { parametersOf(itemId) })
        val state by screenModel.state.collectAsStateWithLifecycle()
        val timelineScreenModel: TimelineScreenModel = koinInject()

        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground

        val snackbarHostState = remember { SnackbarHostState() }
        var refreshAndroidView by remember { mutableStateOf(false) }

        // https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll#parent-compose-child-view
        val bottomBarHeight = 64.dp
        val bottomBarOffsetHeightPx = remember { mutableFloatStateOf(0f) }

        var showTextFormatPopup by remember { mutableStateOf(false) }
        var readabilityState by remember { mutableStateOf(ReadabilityState.OFF) }
        var readableText by remember { mutableStateOf("") }

        val itemJustifyText by preferences.itemJustifyText.flow.collectAsState(initial=false)
        LaunchedEffect(itemJustifyText) {
            refreshAndroidView = true
        }
        val itemTextSizeMultiplier by preferences.itemTextSizeMultiplier.flow.collectAsState(initial=1.0f)
        LaunchedEffect(itemTextSizeMultiplier) {
            refreshAndroidView = true
        }
        val itemLineSizeMultiplier by preferences.itemLineSizeMultiplier.flow.collectAsState(initial=1.0f)
        LaunchedEffect(itemLineSizeMultiplier) {
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

            val accentColor = if (itemWithFeed.color != 0) {
                Color(itemWithFeed.color)
            } else {
                primaryColor
            }

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
                        screenModel.setItemReadState(item.apply { isRead = it }
                        ) { timelineScreenModel.invalidatePagingSource() }
                    },
                    onChangeStarState = {
                        screenModel.setItemStarState(item.apply { isStarred = it })
                    },
                )
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
                                            contentDescription = null
                                        )
                                    }
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
                    AndroidView(
                        factory = { context ->
                            ItemLinearLayout(
                                context = context,
                                useBackgroundTitle = item.imageLink != null,
                                onUrlClick = { url -> openUrl(url) },
                                onImageLongPress = { url -> screenModel.openImageDialog(url) },
                                onPageUpdate = { c: Int, t: Int ->
                                    currentPage = c
                                    totalPages = t
                                }
                            ) {}
                        },
                        update = { linearLayout ->
                            if (refreshAndroidView) {
                                val webView = linearLayout.getChildAt(1) as ItemWebView

                                webView.loadText(
                                    itemWithFeed = itemWithFeed,
                                    accentColor = accentColor,
                                    backgroundColor = backgroundColor,
                                    onBackgroundColor = onBackgroundColor,
                                    justifyText = itemJustifyText,
                                    textSizeMultiplier = itemTextSizeMultiplier,
                                    lineSizeMultiplier = itemLineSizeMultiplier,
                                    readableText = if (readabilityState == ReadabilityState.ON) readableText else ""
                                )

                                refreshAndroidView = false
                            }
                        },
                        modifier = Modifier.matchParentSize()
                    )

                    if (showTextFormatPopup) {
                        MoreOptionsPopup(
                            onDismiss = { showTextFormatPopup = false },
                            onTextSizeSliderValueChange = { newValue ->
                                screenModel.setItemTextSizeMultiplier(newValue)
                            },
                            onLineSizeSliderValueChange = { newValue ->
                                screenModel.setItemLineSizeMultiplier(newValue)
                            },
                            textSizeSliderValue = itemTextSizeMultiplier,
                            lineSizeSliderValue = itemLineSizeMultiplier,
                            isJustified = itemJustifyText,
                            onJustifyToggle = { newValue ->
                                screenModel.setItemJustifyText(newValue)
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
fun MoreOptionsPopup(
    onDismiss: () -> Unit,
    onTextSizeSliderValueChange: (Float) -> Unit,
    onLineSizeSliderValueChange: (Float) -> Unit,
    textSizeSliderValue: Float,
    lineSizeSliderValue: Float,
    isJustified: Boolean,
    onJustifyToggle: (Boolean) -> Unit
) {
    val snapPoints = listOf(1f, 1.25f, 1.5f, 1.75f, 2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = onDismiss,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 8.dp)
                .border(
                    width = 1.dp,
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp)
                ),
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
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
                        steps = 3,
                        modifier = Modifier.width(200.dp)
                    )

                    IconButton(
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
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
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
                        steps = 3,
                        modifier = Modifier.width(200.dp)
                    )

                    IconButton(
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
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatAlignJustify,
                        contentDescription = "Justify Text",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "Justify Text",
                        modifier = Modifier.padding(start = 8.dp, end = 115.dp)
                    )
                    Switch(
                        checked = isJustified,
                        onCheckedChange = onJustifyToggle
                    )
                }
            }
        }
    }
}