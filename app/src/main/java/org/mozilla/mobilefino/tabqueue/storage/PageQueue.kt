package org.mozilla.mobilefino.tabqueue.storage

import android.content.Context
import org.json.JSONArray
import org.mozilla.mobilefino.tabqueue.util.normaliseURL
import java.lang.ref.WeakReference
import java.util.*

/**
 * List of pages that have been queued for reading.
 */
class PageQueue {
    val SHAREDPREFERENCES_NAME = "pagequeue"

    val KEY_QUEUE = "key_sitequeue"

    val mPageList: LinkedHashSet<String>

    val mContext: WeakReference<Context>

    constructor(context: Context) {
        mContext = WeakReference<Context>(context.applicationContext)

        val preferences = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)

        mPageList = LinkedHashSet()

        if (preferences.contains(KEY_QUEUE)) {
            val jsonString = preferences.getString(KEY_QUEUE, "");
            val jsonArray = JSONArray(jsonString)
            // Iterate and insert

            var length = jsonArray.length()
            for (i in 0..(length-1)) {
                mPageList.add(jsonArray.getString(i))
            }
        }
    }

    private fun commit() {
        synchronized(mPageList) {
            val context = mContext.get() ?: return

            val jsonArray = JSONArray()
            for (url in mPageList) {
                jsonArray.put(url)
            }

            val jsonString = jsonArray.toString()

            context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_QUEUE, jsonString)
                    .apply()
        }
    }

    fun add(url: String) {
        synchronized(mPageList) {
            mPageList.add(
                    normaliseURL(url)
            )
        }

        commit()
    }

    fun remove(url: String) {
        synchronized(mPageList) {
            mPageList.remove(url)
        }

        commit()
    }

    fun getPages(): List<String> {
        synchronized(mPageList) {
            return Collections.unmodifiableList(mPageList.toList())
        }
    }
}

private val sPageQueueLock = Object()
private var sPageQueue: PageQueue? = null

fun getPageQueue(context: Context): PageQueue {
    synchronized(sPageQueueLock) {
        if (sPageQueue == null) {
            sPageQueue = PageQueue(context)
        }

        // sPageQueue cannot be null at this point (but the IDE / compiler aren't able to deduce that)
        return sPageQueue!!
    }
}