package com.readrops.api.localfeed

import android.accounts.NetworkErrorException
import androidx.annotation.WorkerThread
import com.gitlab.mvysny.konsumexml.Konsumer
import com.gitlab.mvysny.konsumexml.konsumeXml
import com.readrops.api.localfeed.json.JSONFeedAdapter
import com.readrops.api.utils.ApiUtils
import com.readrops.api.utils.AuthInterceptor
import com.readrops.api.utils.exceptions.ParseException
import com.readrops.api.utils.exceptions.UnknownFormatException
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Item
import com.squareup.moshi.Moshi
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.io.IOException
import java.net.HttpURLConnection

class LocalRSSDataSource(private val httpClient: OkHttpClient) : KoinComponent {

    /**
     * Query RSS url
     * @param url url to query
     * @param headers request headers
     * @return a Feed object with its items
     */
    @Throws(ParseException::class, UnknownFormatException::class, NetworkErrorException::class, IOException::class)
    @WorkerThread
    fun queryRSSResource(url: String, headers: Headers?): Pair<Feed, List<Item>>? {
        get<AuthInterceptor>().credentials = null
        val response = queryUrl(url, headers)

        return when {
            response.isSuccessful -> {
                val pair = parseContent(response, url)

                response.body?.close()
                pair
            }
            response.code == HttpURLConnection.HTTP_NOT_MODIFIED -> null
            else -> throw NetworkErrorException("$url returned ${response.code} code : ${response.message}")
        }
    }

    /**
     * Checks if the provided url is a RSS resource
     * @param url url to check
     * @return true if [url] is a RSS resource, false otherwise
     */
    @WorkerThread
    fun isUrlRSSResource(url: String): Boolean {
        val response = queryUrl(url, null)

        return if (response.isSuccessful) {
            val header = response.header(ApiUtils.CONTENT_TYPE_HEADER)
                    ?: return false

            val contentType = ApiUtils.parseContentType(header)
                    ?: return false

            var type = LocalRSSHelper.getRSSType(contentType)

            if (type == LocalRSSHelper.RSSType.UNKNOWN) {
                val konsumer = response.body!!.byteStream().konsumeXml().apply {
                    val rootKonsumer = nextElement(LocalRSSHelper.RSS_ROOT_NAMES)

                    rootKonsumer?.let { type = LocalRSSHelper.guessRSSType(rootKonsumer) }
                }

                konsumer.close()
            }

            type != LocalRSSHelper.RSSType.UNKNOWN
        } else false
    }

    @Throws(IOException::class)
    private fun queryUrl(url: String, headers: Headers?): Response {
        val requestBuilder = Request.Builder().url(url)
        headers?.let { requestBuilder.headers(it) }

        return httpClient.newCall(requestBuilder.build()).execute()
    }

    private fun parseContent(response: Response, url: String): Pair<Feed, List<Item>> {
        val header = response.header(ApiUtils.CONTENT_TYPE_HEADER)
                ?: throw UnknownFormatException("Unable to get $url content-type")

        val contentType = ApiUtils.parseContentType(header)
                ?: throw ParseException("Unable to parse $url content-type")

        var type = LocalRSSHelper.getRSSType(contentType)

        var konsumer: Konsumer? = null
        if (type != LocalRSSHelper.RSSType.JSONFEED)
            konsumer = response.body!!.byteStream().konsumeXml()

        var rootKonsumer: Konsumer? = null
        // if we can't guess type based on content-type header, we use the content
        if (type == LocalRSSHelper.RSSType.UNKNOWN) {
            konsumer = response.body!!.byteStream().konsumeXml()
            rootKonsumer = konsumer.nextElement(LocalRSSHelper.RSS_ROOT_NAMES)

            if (rootKonsumer != null) {
                type = LocalRSSHelper.guessRSSType(rootKonsumer)
            }
        }

        // if we can't guess type even with the content, we are unable to go further
        if (type == LocalRSSHelper.RSSType.UNKNOWN) throw UnknownFormatException("Unable to guess $url RSS type")

        val feed = parseFeed(rootKonsumer ?: konsumer, type, response)
        //val items = parseItems(ByteArrayInputStream(bodyArray), type)

        rootKonsumer?.finish()
        konsumer?.close()

        return Pair(feed, listOf())
    }

    private fun parseFeed(konsumer: Konsumer?, type: LocalRSSHelper.RSSType, response: Response): Feed {
        val feed = if (type != LocalRSSHelper.RSSType.JSONFEED) {
            val adapter = XmlAdapter.xmlFeedAdapterFactory(type)

            adapter.fromXml(konsumer!!)
        } else {
            val adapter = Moshi.Builder()
                    .add(JSONFeedAdapter())
                    .build()
                    .adapter(Feed::class.java)

            adapter.fromJson(Buffer().readFrom(response.body!!.byteStream()))!!
        }

        handleSpecialCases(feed, type, response)

        feed.etag = response.header(ApiUtils.ETAG_HEADER)
        feed.lastModified = response.header(ApiUtils.LAST_MODIFIED_HEADER)

        return feed
    }

    /*private fun parseItems(stream: InputStream, type: LocalRSSHelper.RSSType): List<Item> {
        return if (type != LocalRSSHelper.RSSType.JSONFEED) {
            val adapter = XmlAdapter.xmlItemsAdapterFactory(type)

            adapter.fromXml(stream)
        } else {
            val adapter = Moshi.Builder()
                    .add(Types.newParameterizedType(MutableList::class.java, Item::class.java), JSONItemsAdapter())
                    .build()
                    .adapter<List<Item>>(Types.newParameterizedType(MutableList::class.java, Item::class.java))

            adapter.fromJson(Buffer().readFrom(stream))!!
        }
    }*/

    private fun handleSpecialCases(feed: Feed, type: LocalRSSHelper.RSSType, response: Response) {
        with(feed) {
            if (type == LocalRSSHelper.RSSType.RSS_2) {
                // if an atom:link element was parsed, we still replace its value as it is unreliable,
                // otherwise we just add the rss url
                url = response.request.url.toString()
            } else if (type == LocalRSSHelper.RSSType.ATOM || type == LocalRSSHelper.RSSType.RSS_1) {
                if (url == null) url = response.request.url.toString()
                if (siteUrl == null) siteUrl = response.request.url.scheme + "://" + response.request.url.host
            }
        }
    }
}