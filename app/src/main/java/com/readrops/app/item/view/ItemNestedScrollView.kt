package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView


/*
@SuppressLint("ResourceType", "ViewConstructor")
class ItemNestedScrollView(
    context: Context,
    useBackgroundTitle: Boolean,
    onGlobalLayoutListener: (viewHeight: Int, contentHeight: Int) -> Unit,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    composeViewContent: @Composable () -> Unit
) : NestedScrollView(context) {

    init {
        addView(
            RelativeLayout(context).apply {
                ViewCompat.setNestedScrollingEnabled(this, false)

                val composeView = ComposeView(context).apply {
                    id = 1

                    setContent {
                        composeViewContent()
                    }
                }

                val composeViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                composeViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                composeView.layoutParams = composeViewParams

                val webView = ItemWebView(
                    context = context,
                    onUrlClick = onUrlClick,
                    onImageLongPress = onImageLongPress
                ).apply {
                    id = 2
                    ViewCompat.setNestedScrollingEnabled(this, true)
                }

                val webViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )

                webViewParams.addRule(RelativeLayout.BELOW, composeView.id)
                webView.layoutParams = webViewParams

                if (useBackgroundTitle) {
                    val density = resources.displayMetrics.density
                    val dpAsPixels = (8 * density + 0.5f).toInt()
                    composeView.setPadding(0, 0, 0, dpAsPixels)
                }

                addView(composeView)
                addView(webView)
            }
        )

        viewTreeObserver.addOnGlobalLayoutListener {
            val viewHeight = this.measuredHeight
            val contentHeight = getChildAt(0).height

            onGlobalLayoutListener(viewHeight, contentHeight)
        }
    }
}
@SuppressLint("ResourceType", "ViewConstructor", "ClickableViewAccessibility")
class ItemNestedScrollView(
    context: Context,
    useBackgroundTitle: Boolean,
    onGlobalLayoutListener: (viewHeight: Int, contentHeight: Int) -> Unit,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    composeViewContent: @Composable () -> Unit
) : NestedScrollView(context) {

    private var startX: Float = 0f
    private var startY: Float = 0f
    private val SWIPE_THRESHOLD = 100

    private var webView: ItemWebView

    init {
        isNestedScrollingEnabled = false // Disable nested scrolling

        val relativeLayout =
            RelativeLayout(context).apply {
                ViewCompat.setNestedScrollingEnabled(this, false) // Disable nested scrolling for RelativeLayout

                val composeView = ComposeView(context).apply {
                    id = 1

                    setContent {
                        composeViewContent()
                    }
                }

                val composeViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                composeViewParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
                composeView.layoutParams = composeViewParams

                webView = ItemWebView(
                    context = context,
                    onUrlClick = onUrlClick,
                    onImageLongPress = onImageLongPress,
                ).apply {
                    id = 2
                    ViewCompat.setNestedScrollingEnabled(this, false) // Disable nested scrolling for WebView
                }

                val webViewParams = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                webViewParams.addRule(RelativeLayout.BELOW, composeView.id)
                webViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                webView.layoutParams = webViewParams

                if (useBackgroundTitle) {
                    val density = resources.displayMetrics.density
                    val dpAsPixels = (8 * density + 0.5f).toInt()
                    composeView.setPadding(0, 0, 0, dpAsPixels)
                }

                addView(composeView)
                addView(webView)
            }

        addView(relativeLayout)

        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val contentHeight = getChildAt(0).height
                viewTreeObserver.removeOnGlobalLayoutListener(this)

                val webViewParams = webView.layoutParams
                webViewParams.height =
                    height - relativeLayout.getChildAt(0).height // Subtract ComposeView height
                webView.layoutParams = webViewParams

                onGlobalLayoutListener(height, contentHeight)
            }
        })

         setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    true
                }
                MotionEvent.ACTION_UP -> {
                    handleTap(event.x)
                    true
                }
                else -> false
            }
        }
    }

    private fun handleTap(x: Float) {
        val width = this.width
        if (x < width / 3) {
            webView.previousPage()
        } else if (x > width * 2 / 3) {
            webView.nextPage()
        }
    }
}
*/

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ItemLinearLayout(
    context: Context,
    useBackgroundTitle: Boolean,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onPageUpdate: (Int, Int) -> Unit,
    composeViewContent: @Composable () -> Unit,
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
        )
        val webViewParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        webViewParams.weight = 1f
        webView.layoutParams = webViewParams

        if (useBackgroundTitle) {
            val density = resources.displayMetrics.density
            val dpAsPixels = (8 * density + 0.5f).toInt()
            composeView.setPadding(0, 0, 0, dpAsPixels)
        }

        addView(composeView)
        addView(webView)

    }
}

