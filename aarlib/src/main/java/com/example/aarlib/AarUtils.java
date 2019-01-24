package com.example.aarlib;

import android.content.Context;
import android.util.Log;

public final class AarUtils {
    private static final String TAG = AarUtils.class.getSimpleName();

    private AarUtils() {
    }

    public static void log(Context context) {
        Log.d(TAG, "log: context=" + context);
    }
}
