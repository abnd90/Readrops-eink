package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ItemLinearLayout(
    context: Context,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onPageUpdate: (Int, Int) -> Unit,
    previousItem: () -> Unit,
    nextItem: () -> Unit,
    webView: ItemWebView
) : LinearLayout(context) {

    init {
        orientation = VERTICAL

        val webViewParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        webViewParams.weight = 1f
        webView.layoutParams = webViewParams

        webView.onResume()
        webView.init(onUrlClick, onImageLongPress, onPageUpdate, previousItem, nextItem)
        addView(webView)
    }
}

