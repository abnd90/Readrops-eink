package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import kotlin.math.abs

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class ItemLinearLayout(
    context: Context,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    onPageUpdate: (Int, Int) -> Unit,
    previousItem: () -> Unit,
    nextItem: () -> Unit,
    private val webView: ItemWebView,
    tbMarginPerc: Int,
    lrMarginPerc: Int
) : LinearLayout(context) {

    private var height = 0
    private var width = 0

    private var lrMarginPerc = 0
    private val gestureDetector: GestureDetector

    fun adjustMargins(tbMarginPerc: Int, lrMarginPerc: Int) {
        this.lrMarginPerc = lrMarginPerc
        if (height != 0 && width != 0) {
            val paddingTbInPx = (height * tbMarginPerc / 100f).toInt()
            setPadding(0, paddingTbInPx, 0, paddingTbInPx)
        }
    }

    init {
        orientation = VERTICAL

        viewTreeObserver.addOnGlobalLayoutListener {
            val oldHeight = this.height
            this.height = this.getHeight()
            this.width = this.getWidth()
            if (oldHeight == 0) {
                adjustMargins(tbMarginPerc, lrMarginPerc)
            }
        }

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 50
                private val SWIPE_VELOCITY_THRESHOLD = 30

                override fun onDown(event: MotionEvent): Boolean {
                    val pageWidth = width
                    val sideMarginWidth = pageWidth * lrMarginPerc / 100
                    when {
                        event.x < sideMarginWidth -> {
                            return true
                        }
                        event.x > pageWidth - sideMarginWidth -> {
                            return true
                        }
                    }
                    return false
                }

                override fun onSingleTapUp(event: MotionEvent): Boolean {
                    val pageWidth = width
                    val sideMarginWidth = pageWidth * lrMarginPerc / 100
                    when {
                        event.x < sideMarginWidth -> {
                            webView.previousPage()
                            return true
                        }
                        event.x > pageWidth - sideMarginWidth -> {
                            webView.nextPage()
                            return true
                        }
                    }
                    return false
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val diffX = e2.x - (e1?.x ?: 0f)
                    val diffY = e2.y - (e1?.y ?: 0f)

                    if (abs(diffX) > abs(diffY) &&
                        abs(diffX) > SWIPE_THRESHOLD &&
                        abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
                    ) {
                        // Swipe left/right
                        if (diffX > 0) {
                            webView.previousPage()
                        } else {
                            webView.nextPage()
                        }
                        return true
                    }

                    if (abs(diffX) < abs(diffY) &&
                        abs(diffY) > 5 * SWIPE_THRESHOLD &&
                        abs(velocityY) > 2 * SWIPE_VELOCITY_THRESHOLD
                    ) {
                        if (diffY > 0) {
                            previousItem()
                        } else {
                            nextItem()
                        }
                        return true
                    }
                    return false
                }
            })

        setOnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }
        webView.setOnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }

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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pageWidth = width
        val sideMarginWidth = pageWidth * lrMarginPerc / 100
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                when {
                    event.x < sideMarginWidth -> {
                        webView.previousPage()
                        return true
                    }
                    event.x > pageWidth - sideMarginWidth -> {
                        webView.nextPage()
                        return true
                    }
                }
            }
            MotionEvent.ACTION_DOWN -> {
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

