package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ItemLinearLayout(
    context: Context,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onPageUpdate: (Int, Int) -> Unit,
    composeViewContent: @Composable () -> Unit,
    previousItem: () -> Unit,
    nextItem: () -> Unit,
) : LinearLayout(context) {

    private var webView: ItemWebView

    init {
        orientation = VERTICAL

        val composeView = ComposeView(context).apply {
            setContent {
                composeViewContent()
            }
        }

        webView = ItemWebView(
            context = context,
            onUrlClick = onUrlClick,
            onImageLongPress = onImageLongPress,
            onPageUpdate = onPageUpdate,
            previousItem = previousItem,
            nextItem = nextItem

        )
        val webViewParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        webViewParams.weight = 1f
        webView.layoutParams = webViewParams

        addView(composeView)
        addView(webView)

    }
}

