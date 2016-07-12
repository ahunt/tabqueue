package org.mozilla.mobilefino.tabqueue

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsCallback
import android.support.customtabs.CustomTabsClient
import android.support.customtabs.CustomTabsIntent
import android.support.customtabs.CustomTabsServiceConnection
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import com.androidzeitgeist.featurizer.Featurizer
import com.androidzeitgeist.featurizer.features.WebsiteFeatures
import org.mozilla.mobilefino.tabqueue.storage.PageInfoFetcher
import org.mozilla.mobilefino.tabqueue.storage.PageInfoReceiver
import org.mozilla.mobilefino.tabqueue.storage.getPageInfoCache
import org.mozilla.mobilefino.tabqueue.storage.getPageQueue
import org.mozilla.mobilefino.tabqueue.util.normaliseURL
import java.util.*

const val FLAG_KEEP = "KEEP_URL"
const val FLAG_NEXT = "NEXT_URL"
const val FLAG_REMOVE_URL = "REMOVE_URL"
const val KEY_LAST_URL = "LAST_OPENED"

class QueueViewActivity : AppCompatActivity() {
    val CUSTOMTAB_PACKAGE = "com.chrome.dev"

    data class PageInfo(val url: String,
                       val title: String?,
                       val description: String?)

    var mFilter: String = ""

    var loaderCallbacks: QueuedPageLoaderCallbacks? = null

    class QueuedPageLoader(context: Context, val filter: String): AsyncTaskLoader<List<String>>(context) {

        override fun loadInBackground(): List<String> {
            val urlList = getPageQueue(getContext()).getPages()

            if (filter.length == 0) {
                return urlList
            }

            // Yes there is probably a faster way of doing this, but this is a prototype
            val filteredList = ArrayList<String>()
            for (url in urlList) {
                if (url.contains(filter, true)) {
                    filteredList.add(url)
                }
            }

            return filteredList;
        }

        // TODO: force reloads using onContentChanged()
        // TODO: first implement callbacks / update notifications from PageQueue
    }

    inner class QueuedPageLoaderCallbacks(context: Context, adapter: PageListAdapter): LoaderManager.LoaderCallbacks<List<String>> {
        var mAdapter: PageListAdapter
        var mContext: Context

        init {
            mAdapter = adapter
            mContext = context.applicationContext
        }

        override fun onLoadFinished(loader: Loader<List<String>>?, data: List<String>) {
            mAdapter.setPages(data)
        }

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<String>>? {
            return QueuedPageLoader(mContext, mFilter)
        }

        override fun onLoaderReset(loader: Loader<List<String>>?) {
            mAdapter.setPages(Collections.emptyList())
        }
    }

    inner class PageViewHolder(v: View): RecyclerView.ViewHolder(v), View.OnClickListener {
        val title: TextView
        val description: TextView
        val link: TextView

        init {
            title = v.findViewById(R.id.page_title) as TextView
            description = v.findViewById(R.id.page_description) as TextView
            link = v.findViewById(R.id.page_url) as TextView
            v.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val url = link.text.toString()
            openCustomTab(url)
        }
    }

    // This is run before onResume(). However it also looks like we don't run onResume if we start a new
    // activity here.
    override fun onNewIntent(intent: Intent) {
        if (intent.hasExtra(FLAG_REMOVE_URL) ||
                intent.hasExtra(FLAG_NEXT)) {
            val pq = getPageQueue(this)

            val preferences = getPreferences(MODE_PRIVATE)
            val lastURL = preferences.getString(KEY_LAST_URL, "")
            pq.remove(lastURL)

            if (intent.hasExtra(FLAG_NEXT)) {
                // TODO: grab the next page, not the first page in the list. (We don't really care
                // for now though.)
                // If we have a filtered set of results we want the next page in the filtered list
                // (we don't support any filtering yet though).
                if (pq.getPages().size > 0) {
                    val nextURL = pq.getPages().first()

                    openCustomTab(nextURL)
                }
            }

            supportLoaderManager.restartLoader(0, null, loaderCallbacks).forceLoad()

            // There's no strict need to clear this URL, however we might as well get rid of it for
            // organisation reasons.
            preferences.edit()
                    .putString(KEY_LAST_URL, null)
                    .apply()
        }

        super.onNewIntent(intent)
    }

    private fun openCustomTab(url: String) {
        val builder = CustomTabsIntent.Builder()

        val pq = getPageQueue(this)
        if (pq.getPages().size > 1) {
            val actionIntentNext = Intent(this, CCTReceiver::class.java)
            actionIntentNext.putExtra(FLAG_NEXT, true)
            val pendingIntentNext = PendingIntent.getBroadcast(this, 0, actionIntentNext, 0)

            builder.setActionButton(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_next), "Next", pendingIntentNext)
        }

        val actionIntentToolbar = Intent(this, CCTReceiver::class.java)
        val pendingIntentToolbar = PendingIntent.getBroadcast(this, 0, actionIntentToolbar, 0)

        val remoteViews = RemoteViews(packageName, R.layout.custom_tab_navigation)
        remoteViews.setViewVisibility(R.id.button_next,
                if (pq.getPages().size > 1)
                    View.VISIBLE
                else
                    View.INVISIBLE
        )
        val ids = intArrayOf(R.id.button_done, R.id.button_next)
        builder.setSecondaryToolbarViews(remoteViews, ids, pendingIntentToolbar)

        val customTabsIntent = builder.build()
        // We (TQ) are probably the default app - we need to explicitly set the browser to be opened.
        // For now we can just hardcode one browser - eventually we'll need a selector.
        customTabsIntent.intent.setPackage(CUSTOMTAB_PACKAGE)

        // Store the last URL to be opened in a preference - this allows us to remove it from the
        // list when we return to the Activity (assuming we don't pass the keep-url flag).
        val preferences = getPreferences(MODE_PRIVATE)
        preferences.edit()
                .putString(KEY_LAST_URL, url)
                .apply()

        customTabsIntent.launchUrl(this@QueueViewActivity, Uri.parse(url));

    }

    class CCTReceiver(): BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val qvIntent = Intent(context, QueueViewActivity::class.java)
            if (intent.hasExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID)) {
                val clickedId = intent.getIntExtra(CustomTabsIntent.EXTRA_REMOTEVIEWS_CLICKED_ID, -1);

                val flag = when (clickedId) {
                    R.id.button_done -> FLAG_REMOVE_URL
                    R.id.button_next -> FLAG_NEXT
                    // Default: do nothing
                    else -> FLAG_KEEP
                }
                qvIntent.putExtra(flag, true)
            } else {
                qvIntent.putExtras(intent?.extras)
            }
            context.startActivity(qvIntent)
        }
    }

    inner class PageListAdapter(): RecyclerView.Adapter<PageViewHolder>() {
        private var mPages: List<String> = Collections.emptyList()

        override fun getItemCount(): Int {
            return mPages.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder? {
            val inflater = LayoutInflater.from(parent.context)

            val pageLayout = inflater.inflate(R.layout.page_list_item, parent, false)
            val pageLayoutVH = PageViewHolder(pageLayout)
            return pageLayoutVH
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val url = mPages.get(position)

            holder.link.text = url
            holder.title.text = null
            holder.description.text = null

            PageInfoFetcher().getPageInfo(url, this@QueueViewActivity, object: PageInfoReceiver {
                override fun processPageInfo(features: WebsiteFeatures) {
                    if (holder.link.text.equals(normaliseURL(features.url))) {
                        holder.title.text = features.title
                        holder.description.text = features.description
                    }
                }

            })
        }

        fun setPages(pages: List<String>) {
            mPages = pages

            notifyDataSetChanged()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue_view)
        val toolbar = findViewById(R.id.toolbar) as Toolbar?
        setSupportActionBar(toolbar)

        val pageList = findViewById(R.id.page_list) as RecyclerView
        var adapter = PageListAdapter()
        pageList.adapter = adapter
        pageList.layoutManager = LinearLayoutManager(this)

        val fab = findViewById(R.id.fab) as FloatingActionButton?
        fab!!.setOnClickListener { view ->
            val addPageDialog = AddPageDialog()
            addPageDialog.show(fragmentManager, "dialog")
        }

        val searchBox = findViewById(R.id.search_box) as EditText
        searchBox.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
                mFilter = text.toString()
                supportLoaderManager.restartLoader(0, null, loaderCallbacks).forceLoad()
            }

        })

        loaderCallbacks = QueuedPageLoaderCallbacks(this, pageList.adapter as PageListAdapter)
        supportLoaderManager.initLoader(0, null, loaderCallbacks).forceLoad()

        val connection = object: CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0)
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
            }

        }

        CustomTabsClient.bindCustomTabsService(this, CUSTOMTAB_PACKAGE, connection)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_queue_view, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
