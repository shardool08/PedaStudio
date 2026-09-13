package com.tippingpoint.pedastudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TlmResourceItem(
    val id: String,
    val label: String,
    val emoji: String,
    val imageUrl: String = "",
) {
    fun displayLabel(lang: String): String = label
}

class TlmResourceCatalog(context: Context) {
    private val items: List<TlmResourceItem>
    private val byId: Map<String, TlmResourceItem>
    private var remoteImageUrls: Map<String, String> = emptyMap()

    init {
        val json = context.assets.open("tlm-catalog.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        val list = mutableListOf<TlmResourceItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                TlmResourceItem(
                    id = o.getString("id"),
                    label = o.getString("label"),
                    emoji = o.optString("emoji", "📦"),
                    imageUrl = o.optString("imageUrl", ""),
                ),
            )
        }
        items = list
        byId = list.associateBy { it.id }
    }

    fun all(): List<TlmResourceItem> = items

    fun get(id: String): TlmResourceItem? = byId[id]

    fun applyRemoteImageUrls(urls: Map<String, String>) {
        remoteImageUrls = urls
    }

    fun emojiFor(id: String): String = byId[id]?.emoji ?: "📦"

    fun imageUrlFor(id: String): String = remoteImageUrls[id].orEmpty().ifBlank { byId[id]?.imageUrl.orEmpty() }

    fun labelFor(id: String): String = byId[id]?.label ?: id.replace('_', ' ')
}
