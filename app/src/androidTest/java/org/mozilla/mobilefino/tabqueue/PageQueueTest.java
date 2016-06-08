package org.mozilla.mobilefino.tabqueue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityTestCase;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;
import android.test.mock.MockContext;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.mobilefino.tabqueue.storage.PageQueue;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PageQueueTest extends AndroidTestCase {

    @Test
    public void test_insertion_succeeds() {
        Context context = InstrumentationRegistry.getContext();
        PageQueue pq = new PageQueue(context);

        assertEquals(pq.getPages().size(), 0);

        final String url = "http://mozilla.org";
        pq.add(url);
        assertEquals(pq.getPages().size(), 1);
        assertEquals(pq.getPages().get(0), url);
    }

    @Test
    public void test_deletion_succeeds() {
        Context context = InstrumentationRegistry.getContext();
        PageQueue pq = new PageQueue(context);

        assertEquals(pq.getPages().size(), 0);

        final String url = "http://mozilla.org";
        pq.add(url);
        assertEquals(pq.getPages().size(), 1);
        pq.remove(url);
        assertEquals(pq.getPages().size(), 0);
    }

}
