package com.s92086882.mydreamplacewishlist;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing SharedPreferences, such as guest login state.
 */
public class SharedPreferencesHelper {

    private static final String PREF_NAME = "auth";
    private static final String KEY_IS_GUEST = "isGuest";

    public static boolean isGuest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_GUEST, true); // default to true
    }

    public static void setGuest(Context context, boolean isGuest) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply();
    }
}