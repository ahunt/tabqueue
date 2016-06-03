package org.mozilla.mobilefino.tabqueue.storage

import android.content.Context
import org.json.JSONArray
import java.lang.ref.WeakReference

val SHAREDPREFERENCES_NAME = "pagequeue"

val KEY_QUEUE = "key_sitequeue"

/**
 * List of pages that have been queued for reading.
 */
class PageQueue {
    val mPageList: JSONArray

    val mContext: WeakReference<Context>

    constructor(context: Context) {
        mContext = WeakReference<Context>(context.applicationContext)

        val preferences = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)

        if (preferences.contains(KEY_QUEUE)) {
            val jsonString = preferences.getString(KEY_QUEUE, "");

            mPageList = JSONArray(jsonString)
        } else {
            mPageList = JSONArray()
        }
    }

    private fun commit() {
        synchronized(mPageList) {
            val context = mContext.get() ?: return

            val jsonString = mPageList.toString()

            context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_QUEUE, jsonString)
                    .apply()
        }
    }

    fun add(url: String) {
        synchronized(mPageList) {
            mPageList.put(url)
        }

        commit()
    }
}

val sPageQueueLock = Object()
var sPageQueue: PageQueue? = null

fun getPageQueue(context: Context): PageQueue {
    synchronized(sPageQueueLock) {
        if (sPageQueue == null) {
            sPageQueue = PageQueue(context)
        }

        // sPageQueue cannot be null at this point (but the IDE / compiler aren't able to deduce that)
        return sPageQueue!!
    }
}