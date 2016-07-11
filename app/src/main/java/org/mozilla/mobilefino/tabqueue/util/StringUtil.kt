package org.mozilla.mobilefino.tabqueue.util

fun normaliseURL(url: String): String {
    if (url.endsWith('/')) {
        return url.substring(0..(url.length - 2))
    } else {
        return url
    }
}