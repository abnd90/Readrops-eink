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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.TextIncrease
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.readrops.app.R
import com.readrops.app.item.view.ItemLinearLayout
import com.readrops.app.item.view.ItemWebView
import com.readrops.app.util.Preferences
import com.readrops.app.util.components.AndroidScreen
import com.readrops.app.util.components.CenteredProgressIndicator
import com.readrops.app.util.components.FeedIcon
import com.readrops.app.util.components.IconText
import com.readrops.app.util.theme.MediumSpacer
import com.readrops.app.util.theme.ShortSpacer
import com.readrops.app.util.theme.spacing
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.util.DateUtils
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

class ItemScreen(
    private val itemId: Int,
) : AndroidScreen() {

    @Composable
    override fun Content() {
        val preferences = koinInject<Preferences>()

        val context = LocalContext.current
        val density = LocalDensity.current
        val navigator = LocalNavigator.currentOrThrow

        val screenModel =
            getScreenModel<ItemScreenModel>(parameters = { parametersOf(itemId) })
        val state by screenModel.state.collectAsStateWithLifecycle()

        val primaryColor = MaterialTheme.colorScheme.primary
        val backgroundColor = MaterialTheme.colorScheme.background
        val onBackgroundColor = MaterialTheme.colorScheme.onBackground

        val snackbarHostState = remember { SnackbarHostState() }
        val isScrollable by remember { mutableStateOf(true) }
        var refreshAndroidView by remember { mutableStateOf(true) }

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
                            if (isScrollable) {
                                IntOffset(
                                    x = 0,
                                    y = -bottomBarOffsetHeightPx.floatValue.roundToInt()
                                )
                            } else {
                                IntOffset(0, 0)
                            }
                        },
                    pageInfo = Pair(currentPage, totalPages),
                    onShare = { screenModel.shareItem(item, context) },
                    onOpenUrl = { openUrl(item.link!!) },
                    onChangeReadState = {
                        screenModel.setItemReadState(item.apply { isRead = it })
                    },
                    onChangeStarState = {
                        screenModel.setItemStarState(item.apply { isStarred = it })
                    },
                    readabilityState = readabilityState,
                    onReadability =  {
                        showReadable ->
                        if (showReadable && readableText.isEmpty()) {
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
                                if (showReadable) ReadabilityState.ON else ReadabilityState.OFF
                            refreshAndroidView = true
                        }
                    }
                )
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = bottomBar
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
                            ) {
                                if (item.imageLink != null) {
                                    BackgroundTitle(itemWithFeed = itemWithFeed)
                                } else {
                                    Box {
                                        IconButton(
                                            onClick = { navigator.pop() },
                                            modifier = Modifier
                                                .statusBarsPadding()
                                                .align(Alignment.TopStart)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                                contentDescription = null,
                                            )
                                        }

                                        IconButton(
                                            onClick = { showTextFormatPopup = !showTextFormatPopup },
                                            modifier = Modifier
                                                .statusBarsPadding()
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.TextFormat,
                                                contentDescription = "Text Formatting"
                                            )
                                        }
                                    }
                                }
                            }
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
fun BackgroundTitle(
    itemWithFeed: ItemWithFeed,
) {
    val navigator = LocalNavigator.currentOrThrow

    val onScrimColor = Color.White.copy(alpha = 0.85f)
    val accentColor = if (itemWithFeed.color != 0) {
        Color(itemWithFeed.color)
    } else {
        onScrimColor
    }

    Surface(
        shape = RoundedCornerShape(
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        ),
        modifier = Modifier.height(IntrinsicSize.Max)
    ) {
        AsyncImage(
            model = itemWithFeed.item.imageLink,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.ic_broken_image),
            modifier = Modifier
                .fillMaxSize()
        )

        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box {
                IconButton(
                    onClick = { navigator.pop() },
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                }

                SimpleTitle(
                    itemWithFeed = itemWithFeed,
                    titleColor = onScrimColor,
                    accentColor = accentColor,
                    baseColor = onScrimColor,
                    bottomPadding = true
                )
            }
        }
    }

    MediumSpacer()
}

@Composable
fun SimpleTitle(
    itemWithFeed: ItemWithFeed,
    titleColor: Color,
    accentColor: Color,
    baseColor: Color,
    bottomPadding: Boolean,
) {
    val item = itemWithFeed.item
    val spacing = MaterialTheme.spacing.mediumSpacing

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing,
                end = spacing,
                top = spacing,
                bottom = if (bottomPadding) spacing else 0.dp
            )
    ) {
        FeedIcon(
            iconUrl = itemWithFeed.feedIconUrl,
            name = itemWithFeed.feedName,
            size = 48.dp,
            modifier = Modifier.clip(CircleShape)
        )

        ShortSpacer()

        Text(
            text = itemWithFeed.feedName,
            style = MaterialTheme.typography.labelLarge,
            color = baseColor,
            textAlign = TextAlign.Center
        )

        ShortSpacer()

        Text(
            text = item.title!!,
            style = MaterialTheme.typography.headlineMedium,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (item.author != null) {
            ShortSpacer()

            IconText(
                icon = painterResource(id = R.drawable.ic_person),
                text = itemWithFeed.item.author!!,
                style = MaterialTheme.typography.labelMedium,
                color = baseColor,
                tint = accentColor
            )
        }

        ShortSpacer()

        val readTime = if (item.readTime > 1) {
            stringResource(id = R.string.read_time, item.readTime.roundToInt())
        } else {
            stringResource(id = R.string.read_time_lower_than_1)
        }
        Text(
            text = "${DateUtils.formattedDate(item.pubDate!!)} ${stringResource(id = R.string.interpoint)} $readTime",
            style = MaterialTheme.typography.labelMedium,
            color = baseColor
        )
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
                .padding(top = 56.dp, end = 8.dp)
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