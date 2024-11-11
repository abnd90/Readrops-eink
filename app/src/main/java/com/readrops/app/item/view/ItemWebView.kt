package com.readrops.app.item.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
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
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


class WebAppInterface(private val onPageCountUpdate: (Int) -> Unit,
    private val nextItem: () -> Unit,
    private val prevItem: () -> Unit){
    @JavascriptInterface
    fun setPageCount(pageCount: Int) {
        onPageCountUpdate(pageCount)
    }

    @JavascriptInterface
    fun gotoItem(dir: Int) {
        if (dir > 0)
            nextItem()
        else
            prevItem()
    }
}

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class ItemWebView(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    private var onUrlClick: (String) -> Unit = {}
    private var onImageLongPress: (String) -> Unit = {}
    private var onPageUpdate: (Int, Int) -> Unit = {_, _ ->}
    private var previousItem: () -> Unit = {}
    private var nextItem: () -> Unit = {}

    var currentPage: Int = 0
    var totalPages: Int = 0
    var pageUpdated = {onPageUpdate(currentPage, totalPages)}

    private val sideMarginPerc = 10
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

        addJavascriptInterface(WebAppInterface (onPageCountUpdate = { pageCount ->
            totalPages = pageCount
            if (currentPage >= totalPages) {
                lastPage()
            }
            pageUpdated()
        }, nextItem = { this.nextItem() }, prevItem = { this.previousItem() }), "Android")

        if (0 != (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchX = event.x
                val pageWidth = width
                val sideMarginWidth = pageWidth * sideMarginPerc / 100

                when {
                    touchX < sideMarginWidth -> {
                        previousPage()
                        return true
                    }
                    touchX > pageWidth - sideMarginWidth -> {
                        nextPage()
                        return true
                    }
                }
            }
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        return super.onTouchEvent(event)
    }

    public override fun overScrollBy(
        deltaX: Int, deltaY: Int, scrollX: Int, scrollY: Int,
        scrollRangeX: Int, scrollRangeY: Int, maxOverScrollX: Int,
        maxOverScrollY: Int, isTouchEvent: Boolean
    ): Boolean {
        return false
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
        font: FontPreference,
        nextItem: ItemWithFeed?,
        prevItem: ItemWithFeed?
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
            sanitizeHtml(readableText, itemWithFeed.websiteUrl)
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

        val itemLinksHtml = StringBuilder()

        if (prevItem != null) {
            itemLinksHtml.append(
                "<div id=\"_previousarticle\"><a href=\"#\" onclick=\"Android.gotoItem(-1);\">&lsaquo; Previous Article</a><p>${prevItem.item.title}</p></div>"
            )
        }

        if (nextItem != null) {
            itemLinksHtml.append(
                "<div id=\"_nextarticle\"><a href=\"#\" onclick=\"Android.gotoItem(+1);\">Next Article &rsaquo;</a><p>${nextItem.item.title}</p></div>"
            )
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
            fontFamily,
            itemLinksHtml,
            "$sideMarginPerc"
        )

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

            itemWithFeed.websiteUrl?.let { applyPerWebsiteQuirks(it, document) }
            return sanitizeDoc(document).body().html()
        } else {
            ""
        }
    }

    private fun applyPerWebsiteQuirks(uri: String, document: Document) {
        if (uri.contains("xkcd.com") == true) {
            document.select("img[title]").forEach {
                val figure: Element = Element("figure")
                val figcaption: Element =
                    Element("figcaption").text(it.attr("title"))

                it.wrap(figure.outerHtml());
                it.after(figcaption);
            }
        }
    }

    private fun sanitizeHtml(html: String, baseUrl: String?): String {
        var document = if (baseUrl != null) Jsoup.parse(html, baseUrl) else Jsoup.parse(html)
        return sanitizeDoc(document).body().html()
    }

    private fun sanitizeDoc(document: Document): Document {
        // TODO: CSP sanitize HTML. Current method removes various text elements (Eg: image captions)
        // and is too strict.
        val cleaner = Cleaner(Safelist.relaxed())
        val cleanDoc = cleaner.clean(document)
        return document
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

    private fun firstPage() {
        currentPage = 0
        goToPage(0)
    }

    private fun lastPage() {
        currentPage = totalPages - 1
        goToPage(currentPage)
    }

    private fun goToPage(page:Int) {
        super.scrollTo(page * width, 0)
        pageUpdated()
    }

    fun init(onUrlClick: (String) -> Unit, onImageLongPress: (String) -> Unit, onPageUpdate: (Int, Int) -> Unit, previousItem: () -> Unit, nextItem: () -> Unit) {
        this.onUrlClick = onUrlClick
        this.onImageLongPress = onImageLongPress
        this.onPageUpdate = onPageUpdate
        this.previousItem = previousItem
        this.nextItem = nextItem

        totalPages = 0
        firstPage()
    }
}