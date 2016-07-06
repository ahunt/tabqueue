package org.mozilla.mobilefino.tabqueue.ingestion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.mozilla.mobilefino.tabqueue.storage.getPageQueue

/**
 * A basic activity that handles incoming URLs.
 *
 * We can gobble up all URLs here, and process them as needed.
 */
class URLReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val url: String
        val dataString: String? = intent.dataString

        if (dataString != null) {
            url = dataString
        } else {
            if (!intent.hasExtra(Intent.EXTRA_TEXT)) {
                return
            }
            url = intent.getStringExtra(Intent.EXTRA_TEXT)
        }

        val queue = getPageQueue(this)
        queue.add(url)

        Toast.makeText(this, "URL saved: " + url, Toast.LENGTH_LONG).show()

        finish()
    }
}