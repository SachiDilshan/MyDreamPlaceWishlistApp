package com.s92086882.mydreamplacewishlist;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility wrapper for app-wide SharedPreferences.
 * -
 * Responsibilities:
 * - Centralizes access to authentication state (guest vs logged-in).
 * - Reduces code duplication (instead of calling getSharedPreferences everywhere).
 * -
 * Notes:
 * - PREF_NAME = "auth" → stored as a private XML file (auth.xml) in app's shared_prefs directory.
 * - KEY_IS_GUEST → boolean flag:
 *      true  = guest user (local SQLite storage).
 *      false = authenticated user (Firestore/Firebase storage).
 * -
 * Default behavior:
 * - If preference not set, we assume guest (safe fallback).
 */
public class SharedPreferencesHelper {

    private static final String PREF_NAME = "auth"; // file name for SharedPreferences
    private static final String KEY_IS_GUEST = "isGuest"; // key for guest flag

    /** Returns whether the current user is a guest (default = true). */
    public static boolean isGuest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_GUEST, true); // default to true
    }

    /** Sets guest state (true = guest, false = logged-in). */
    public static void setGuest(Context context, boolean isGuest) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_GUEST, isGuest).apply();
    }
}