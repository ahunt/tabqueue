package org.mozilla.mobilefino.tabqueue.storage

import android.content.Context
import android.os.AsyncTask
import com.androidzeitgeist.featurizer.Featurizer
import com.androidzeitgeist.featurizer.features.WebsiteFeatures
import java.io.IOException

interface PageInfoReceiver {
    fun processPageInfo(features: WebsiteFeatures)
}

class PageInfoFetcher {
    fun getPageInfo(url: String, context: Context, receiver: PageInfoReceiver) {
        val cache = getPageInfoCache(context)

        synchronized(cache) {
            val info = cache.getPageInfo(url)
            if (info != null) {
                receiver.processPageInfo(info)
                return
            }
        }

        // We could have a more sane retrieval process here (to avoid spamming N background tasks),
        // but this is good enough for now
        val task = object: AsyncTask<String, Void, WebsiteFeatures>() {
            override fun doInBackground(vararg urls: String): WebsiteFeatures {
                val url = urls[0]
                val featurizer = Featurizer()
                try {
                    val features = featurizer.featurize(url)
                    // Only store data if we are able to retrieve it. We can fail if there's no network,
                    // and we don't want to cache our faked data which is created in case of errors.
                    cache.putPageInfo(url, features)
                    return features
                } catch (e: IllegalArgumentException) {
                } catch (e: IOException) {
                }

                val builder = WebsiteFeatures.Builder()
                builder.setTitle(url)
                builder.setUrl(url)

                return builder.build()
            }

            override fun onPostExecute(result: WebsiteFeatures) {
                receiver.processPageInfo(result)
            }
        }

        task.execute(url)
    }


}