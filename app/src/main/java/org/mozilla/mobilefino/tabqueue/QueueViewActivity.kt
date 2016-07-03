package org.mozilla.mobilefino.tabqueue

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.LoaderManager
import android.support.v4.content.AsyncTaskLoader
import android.support.v4.content.Loader
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.TextView
import android.widget.Toast
import org.mozilla.mobilefino.tabqueue.storage.getPageQueue
import java.util.*

const val FLAG_KEEP_URL = "KEEP_URL"
const val FLAG_NEXT = "NEXT_URL"
const val KEY_LAST_URL = "LAST_OPENED"

class QueueViewActivity : AppCompatActivity() {
    class QueuedPageLoader(context: Context): AsyncTaskLoader<List<String>>(context) {

        override fun loadInBackground(): List<String> {
            return getPageQueue(getContext()).getPages()
        }

        // TODO: force reloads using onContentChanged()
        // TODO: first implement callbacks / update notifications from PageQueue
    }

    class QueuedPageLoaderCallbacks(context: Context, adapter: PageListAdapter): LoaderManager.LoaderCallbacks<List<String>> {
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
            return QueuedPageLoader(mContext)
        }

        override fun onLoaderReset(loader: Loader<List<String>>?) {
            mAdapter.setPages(Collections.emptyList())
        }
    }

    inner class PageViewHolder(v: View): RecyclerView.ViewHolder(v), View.OnClickListener {
        val title: TextView

        init {
            title = v.findViewById(R.id.page_title) as TextView
            v.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val url = title.text.toString()
            openCustomTab(url)
        }
    }

    // This is run before onResume(). However it also looks like we don't run onResume if we start a new
    // activity here.
    override fun onNewIntent(intent: Intent) {
        if (intent.hasExtra(FLAG_KEEP_URL)) {
            // Clear the last URL so that it isn't removed from the list in onResume
            // This is hacky, there's certainly some potential for a better architecture
            val preferences = getPreferences(MODE_PRIVATE)
            preferences.edit()
                    .putString(KEY_LAST_URL, null)
                    .apply()
        } else if (intent.hasExtra(FLAG_NEXT)) {
            val pq = getPageQueue(this)

            val preferences = getPreferences(MODE_PRIVATE)
            val lastURL = preferences.getString(KEY_LAST_URL, "")
            pq.remove(lastURL)

            // TODO: in future we need to adapt the custom tab to not show the next button if
            // there are no more pages in the list.
            // TODO: grab the next page, not the first page in the list. (We don't really care
            // for now though.)
            if (pq.getPages().size > 0) {
                val nextURL = pq.getPages().first()

                openCustomTab(nextURL)
            }
        }

        super.onNewIntent(intent)
    }

    private fun openCustomTab(url: String) {
        val builder = CustomTabsIntent.Builder()

        val actionIntent = Intent(applicationContext, CCTReceiver::class.java)
        actionIntent.putExtra(FLAG_NEXT, true)

        val pq = getPageQueue(this)
        if (pq.getPages().size > 1) {
            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, actionIntent, 0)
            builder.setActionButton(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_next), "Next", pendingIntent)
        }

        val actionIntentKeep = Intent(applicationContext, CCTReceiver::class.java)
        actionIntentKeep.putExtra(FLAG_KEEP_URL, true)
        val pendingIntentKeep = PendingIntent.getBroadcast(applicationContext, 1, actionIntentKeep, 0)
        builder.addMenuItem("Keep for later", pendingIntentKeep)

        val customTabsIntent = builder.build()
        // We (TQ) are probably the default app - we need to explicitly set the browser to be opened.
        // For now we can just hardcode one browser - eventually we'll need a selector.
        customTabsIntent.intent.setPackage("com.chrome.dev")

        // Store the last URL to be opened in a preference - this allows us to remove it from the
        // list when we return to the Activity (assuming we don't pass the keep-url flag).
        val preferences = getPreferences(MODE_PRIVATE)
        preferences.edit()
                .putString(KEY_LAST_URL, url)
                .apply()

        customTabsIntent.launchUrl(this@QueueViewActivity, Uri.parse(url));

    }

    class CCTReceiver(): BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val qvIntent = Intent(context, QueueViewActivity::class.java)
            qvIntent.putExtras(intent?.extras)
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
            holder.title.text = mPages.get(position)
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
        fab!!.setOnClickListener { view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show() }
    }

    override fun onResume() {
        // TODO: this is broken (and hard to understand)
        // We could also receive onResume if the app is killed (while a custom tab is open)
        // and restarted, leading to the tab being lost. Therefore it's ultimately better to not
        // delete any data with the X button (which might be better UX in general).
        // Our previous assumption was that we'd receive onResume (with a LAST_URL set) only when
        // the user uses X to exit the custom tab (we don't receive any callbacks in that case).

        super.onResume()

        val pageList = findViewById(R.id.page_list) as RecyclerView

        val loaderCallbacks = QueuedPageLoaderCallbacks(this, pageList.adapter as PageListAdapter)
        supportLoaderManager.initLoader(0, null, loaderCallbacks).forceLoad()

        val preferences = getPreferences(MODE_PRIVATE)
        val url = preferences.getString(KEY_LAST_URL, null)

        if (url != null) {
            val pq = getPageQueue(this)

            preferences.edit()
                    .putString(KEY_LAST_URL, null)
                    .apply()

            if (!intent.hasExtra(FLAG_KEEP_URL)) {
                pq.remove(url)
            }
        }
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
