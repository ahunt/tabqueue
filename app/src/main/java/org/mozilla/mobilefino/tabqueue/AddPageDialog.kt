package org.mozilla.mobilefino.tabqueue

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import com.androidzeitgeist.featurizer.Featurizer
import com.androidzeitgeist.featurizer.features.WebsiteFeatures
import org.mozilla.mobilefino.tabqueue.storage.getPageQueue
import java.io.IOException

class AddPageDialog: DialogFragment() {

    var urlEntry: EditText? = null
    var title: TextView? = null
    var description: TextView? = null
    var link: TextView? = null

    inner class InfoUpdater(): AsyncTask<String, Void, WebsiteFeatures?>() {
        override fun doInBackground(vararg url: String): WebsiteFeatures? {
            val featurizer = Featurizer()
            try {
                return featurizer.featurize(url[0])
            } catch (e: IllegalArgumentException) {
                return null
            } catch (e: IOException) {
                return null
            }
        }

        override fun onPostExecute(result: WebsiteFeatures?) {
            title?.setText(result?.title)
            description?.setText(result?.description)
            link?.setText(result?.url)
        }
    }

    inner class LoaderDelay : Runnable {
        var url: String = ""

        fun update(url: String) {
            this.url = url
        }

        override fun run() {
            InfoUpdater().execute(url)
        }

    }

    inner class TextListener: TextWatcher {
        val handler = Handler()
        val pageInfoUpdater = LoaderDelay()

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            title?.setText(null)
            description?.setText(null)
            link?.setText(null)

            handler.removeCallbacks(pageInfoUpdater)
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            val url = editable.toString()
            pageInfoUpdater.update(url)
            handler.postDelayed(pageInfoUpdater, 500)
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog? {
        val builder = AlertDialog.Builder(getActivity())

        builder.setTitle(R.string.addpage_title)
        builder.setPositiveButton(R.string.dialog_save,
                { dialog, button ->
                    val pq = getPageQueue(getActivity())
                    val url = link?.text.toString()
                    pq.add(url)
                    dialog.dismiss()
                } )
        builder.setNegativeButton(R.string.dialog_cancel,
                { dialog, button ->
                    dialog.dismiss()
                } )

        val v = getActivity().layoutInflater.inflate(R.layout.dialog_add_page, null)
        builder.setView(v)

        val dialog = builder.create()

        urlEntry = v.findViewById(R.id.enter_url) as EditText
        title = v.findViewById(R.id.page_title) as TextView
        description = v.findViewById(R.id.page_description) as TextView
        link = v.findViewById(R.id.page_url) as TextView

        urlEntry?.addTextChangedListener(TextListener())

        return dialog
    }
}