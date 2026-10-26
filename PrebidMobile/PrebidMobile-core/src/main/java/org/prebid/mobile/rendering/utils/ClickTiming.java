package org.prebid.mobile.rendering.utils;

import android.os.SystemClock;
import android.util.Log;

/**
 * TEMPORARY (Teads): click-to-browser latency instrumentation.
 * Filter with: adb logcat -s TeadsClickTiming
 */
public final class ClickTiming {

    private static final String TAG = "TeadsClickTiming";
    private static final long STALE_AFTER_MS = 5000;

    private static volatile long startMs;

    private ClickTiming() {}

    /** Starts a new click timeline, unless one started in the last few seconds. */
    public static void start(String step) {
        long now = SystemClock.elapsedRealtime();
        if (now - startMs > STALE_AFTER_MS) {
            startMs = now;
            Log.d(TAG, "----- click -----");
        }
        mark(step);
    }

    public static void mark(String step) {
        long elapsed = SystemClock.elapsedRealtime() - startMs;
        Log.d(TAG, "+" + elapsed + "ms " + step + " [" + Thread.currentThread().getName() + "]");
    }
}
