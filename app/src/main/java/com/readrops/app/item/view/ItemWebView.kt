package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.text.layoutDirection
import com.readrops.app.R
import com.readrops.app.util.FontPreference
import com.readrops.app.util.Utils
import com.readrops.db.pojo.ItemWithFeed
import com.readrops.db.util.DateUtils
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class WebAppInterface(private val onPageCountUpdate: (Int) -> Unit){
    @JavascriptInterface
    fun setPageCount(pageCount: Int) {
        onPageCountUpdate(pageCount)
    }
}

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class ItemWebView(
    context: Context,
    onUrlClick: (String) -> Unit,
    onImageLongPress: (String) -> Unit,
    attrs: AttributeSet? = null,
    onPageUpdate: (Int, Int) -> Unit,
    previousItem: () -> Unit,
    nextItem: () -> Unit,
) : WebView(context, attrs) {

    var currentPage: Int = 0
    var totalPages: Int = 0
    var onPageUpdate = {onPageUpdate(currentPage, totalPages)}
    private val gestureDetector: GestureDetector

    init {
        settings.javaScriptEnabled = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setSupportZoom(false)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { onUrlClick(it) }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }
        }

        setOnLongClickListener {
            val type = hitTestResult.type
            if (type == HitTestResult.IMAGE_TYPE || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                hitTestResult.extra?.let { onImageLongPress(it) }
            }
            false
        }

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                private val SWIPE_THRESHOLD = 50
                private val SWIPE_VELOCITY_THRESHOLD = 30

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
                            previousPage()
                        } else {
                            nextPage()
                        }
                        return true
                    }

                    if (abs(diffX) < abs(diffY) &&
                        abs(diffY) > 3 * SWIPE_THRESHOLD &&
                        abs(velocityY) > SWIPE_VELOCITY_THRESHOLD
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

        addJavascriptInterface(WebAppInterface { pageCount ->
            totalPages = pageCount
            //goToPage(currentPage)
            onPageUpdate()
        }, "Android")
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun loadText(
        itemWithFeed: ItemWithFeed,
        accentColor: Color,
        backgroundColor: Color,
        onBackgroundColor: Color,
        justifyText: Boolean,
        textSizeMultiplier: Float,
        lineSizeMultiplier: Float,
        readableText: String,
        font: FontPreference
    ) {
        val direction = if (Locale.getDefault().layoutDirection == LAYOUT_DIRECTION_LTR) {
            "ltr"
        } else {
            "rtl"
        }

        val dateString = DateUtils.formattedDate(itemWithFeed.item.pubDate!!)
        val textAlign = if (justifyText) {
            "justify"
        } else if (direction == "ltr") {
            "left"
        } else {
            "right"
        }

        val html = if (!readableText.isEmpty()) {
            readableText
        } else {
            formatText(itemWithFeed)
        }
        val readTimeInt =
            if (!readableText.isEmpty()) {
                Utils.readTimeFromString(html).roundToInt()
            } else {
                itemWithFeed.item.readTime.roundToInt()

            }
        val readTime = if (readTimeInt > 1) {
            context.getString(R.string.read_time, readTimeInt.toString())
        } else {
            context.getString(R.string.read_time_lower_than_1)
        }
        val fontFamily = fontPreferenceToCssFamily(font)
        var subHeading = itemWithFeed.feedName
        if (itemWithFeed.item.author != null) {
            subHeading +=
                " · ${itemWithFeed.item.author}"
        }

        // TODO: Find or write a templating library
        val string = context.getString(
            R.string.webview_html_template,
            Utils.getCssColor(accentColor.toArgb()),
            Utils.getCssColor(onBackgroundColor.toArgb()),
            Utils.getCssColor(backgroundColor.toArgb()),
            direction,
            html,
            itemWithFeed.item.title,
            "$dateString · $readTime",
            itemWithFeed.feedIconUrl,
            subHeading,
            textAlign,
            "${textSizeMultiplier}em",
            "${lineSizeMultiplier}em",
            fontFamily
        )

        // TODO: Retain currentPage and scroll to that page.
        currentPage = 0
        loadDataWithBaseURL(
            "file:///android_asset/",
            string,
            "text/html; charset=utf-8",
            "UTF-8",
            null
        )
    }

    private fun fontPreferenceToCssFamily(font: FontPreference): String {
        when (font) {
            FontPreference.SANS_SERIF -> return "sans-serif"
            FontPreference.SERIF -> return "serif"
            FontPreference.MONOSPACE -> return "monospace"
            FontPreference.NEWSREADER -> return "Newsreader"
        }
    }

    private fun formatText(itemWithFeed: ItemWithFeed): String {
        return if (itemWithFeed.item.text != null) {
            val document = if (itemWithFeed.websiteUrl != null) Jsoup.parse(
                Parser.unescapeEntities(itemWithFeed.item.text, false), itemWithFeed.websiteUrl
            ) else Jsoup.parse(
                Parser.unescapeEntities(itemWithFeed.item.text, false)
            )

            document.select("div,span").forEach { it.clearAttributes() }
            return document.body().html()
        } else {
            ""
        }
    }

    fun nextPage() {
        if (currentPage + 1 < totalPages) {
            goToPage(++currentPage)
        }
    }

    fun previousPage() {
        if (currentPage > 0) {
            goToPage(--currentPage)
        }
    }

    fun goToPage(page:Int) {
        evaluateJavascript("window.changePage(${page});", null)
        onPageUpdate()
    }
}