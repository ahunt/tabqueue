package org.mozilla.mobilefino.tabqueue

import android.app.PendingIntent
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
import org.mozilla.mobilefino.tabqueue.storage.getPageQueue
import java.util.*

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
            val url = title.text

            val builder = CustomTabsIntent.Builder()

            // TODO: do something with the pending intents
            // TODO 2: detect exit (via X button)
            // TODO 3: use a checkmark instead of the X button?
            builder.addMenuItem("Keep for later", this@QueueViewActivity.createPendingResult(0, Intent(), 0))
            builder.setActionButton(BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_media_next), "Next", this@QueueViewActivity.createPendingResult(0, Intent(), 0))

            val customTabsIntent = builder.build()
            // We (TQ) are probably the default app - we need to explicitly set the browser to be opened.
            // For now we can just hardcode one browser - eventually we'll need a selector.
            customTabsIntent.intent.setPackage("com.chrome.dev")
            customTabsIntent.launchUrl(this@QueueViewActivity, Uri.parse(url.toString()));
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
        super.onResume()

        val pageList = findViewById(R.id.page_list) as RecyclerView

        val loaderCallbacks = QueuedPageLoaderCallbacks(this, pageList.adapter as PageListAdapter)
        supportLoaderManager.initLoader(0, null, loaderCallbacks).forceLoad()
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
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
