package org.mozilla.mobilefino.tabqueue.ingestion

import android.app.Activity
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

        val url = getIntent().dataString

        val queue = getPageQueue(this)
        queue.add(url)

        Toast.makeText(this, "URL saved: " + url, Toast.LENGTH_LONG).show()

        finish()
    }
}