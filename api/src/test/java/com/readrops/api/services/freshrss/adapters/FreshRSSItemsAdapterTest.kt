package com.readrops.api.services.freshrss.adapters

import com.readrops.api.TestUtils
import com.readrops.db.entities.Item
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import okio.Buffer
import org.joda.time.LocalDateTime
import org.junit.Test

class FreshRSSItemsAdapterTest {

    private val adapter = Moshi.Builder()
            .add(Types.newParameterizedType(List::class.java, Item::class.java), FreshRSSItemsAdapter())
            .build()
            .adapter<List<Item>>(Types.newParameterizedType(List::class.java, Item::class.java))

    @Test
    fun validItemsTest() {
        val stream = TestUtils.loadResource("services/freshrss/adapters/items.json")

        val items = adapter.fromJson(Buffer().readFrom(stream))!!

        with(items[0]) {
            assertEquals(remoteId, "tag:google.com,2005:reader/item/0005c62466ee28fe")
            assertEquals(title, "GNOME’s Default Theme is Getting a Revamp")
            assertNotNull(content)
            assertEquals(link, "http://feedproxy.google.com/~r/d0od/~3/4Zk-fncSuek/adwaita-borderless-theme-in-development-gnome-41")
            assertEquals(author, "Joey Sneddon")
            assertEquals(pubDate, LocalDateTime(1625234040 * 1000L))
            assertEquals(isRead, false)
            assertEquals(isStarred, false)
        }

        with(items[1]) {
            assertEquals(isRead, true)
            assertEquals(isStarred, true)
        }
    }

}