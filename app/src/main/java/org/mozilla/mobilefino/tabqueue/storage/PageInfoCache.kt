package org.mozilla.mobilefino.tabqueue.storage

import android.content.Context
import com.androidzeitgeist.featurizer.features.WebsiteFeatures
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

class PageInfoCache {
    val SHAREDPREFERENCES_NAME = "pageinfos"

    val KEY_PAGEINFOS = "key_pageinfos"

    val KEY_URL = "url"
    val KEY_TITLE = "title"
    val KEY_DESCRIPTION = "description"

    val mPageInfos: HashMap<String, WebsiteFeatures>

    val mContext: WeakReference<Context>

    constructor(context: Context) {
        mContext = WeakReference<Context>(context.applicationContext)

        val preferences = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)

        mPageInfos = HashMap()

        if (preferences.contains(KEY_PAGEINFOS)) {
            val jsonString = preferences.getString(KEY_PAGEINFOS, "");
            val jsonArray = JSONArray(jsonString)
            // Iterate and insert

            var length = jsonArray.length()
            for (i in 0..(length-1)) {
                val item = jsonArray.getJSONObject(i)

                val featuresBuilder = WebsiteFeatures.Builder()

                featuresBuilder.setUrl(item.getString(KEY_URL))
                if (item.has(KEY_TITLE)) {
                    featuresBuilder.setTitle(item.getString(KEY_TITLE))
                }
                if (item.has(KEY_DESCRIPTION)) {
                    featuresBuilder.setDescription(item.getString(KEY_DESCRIPTION))
                }

                val features = featuresBuilder.build()
                mPageInfos.put(features.url, features)
            }
        }
    }

    private fun commit() {
        synchronized(mPageInfos) {
            val context = mContext.get() ?: return

            val jsonArray = JSONArray()
            for (url in mPageInfos.keys) {
                // The compiler is unable to deduce this, but this item must be retrievable
                // modulo bugs in HashMap
                val features = mPageInfos.get(url) as WebsiteFeatures
                val item = JSONObject()
                item.put(KEY_URL, url)
                item.put(KEY_TITLE, features.getTitle())
                item.put(KEY_DESCRIPTION, features.getDescription())

                jsonArray.put(item)
            }

            val jsonString = jsonArray.toString()

            context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PAGEINFOS, jsonString)
                    .apply()
        }
    }

    fun getPageInfo(url: String): WebsiteFeatures? {
        synchronized(mPageInfos) {
            return mPageInfos.get(url)
        }
    }

    fun putPageInfo(url: String, features: WebsiteFeatures) {
        synchronized(mPageInfos) {
            mPageInfos.put(url, features)
            commit()
        }
    }
}

private val sPageInfoCacheLock = Object()
private var sPageInfoCache: PageInfoCache? = null

fun getPageInfoCache(context: Context): PageInfoCache {
    synchronized(sPageInfoCacheLock) {
        if (sPageInfoCache == null) {
            sPageInfoCache = PageInfoCache(context)
        }

        // sPageInfoCache cannot be null at this point (but the IDE / compiler aren't able to deduce that)
        return sPageInfoCache!!
    }
}