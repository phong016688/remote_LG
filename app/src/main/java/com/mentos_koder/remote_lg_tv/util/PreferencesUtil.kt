package com.mentos_koder.remote_lg_tv.util

import android.content.Context

private const val SHARED_PREFS_NAME = "MyPrefs"

fun restoreSwitchState(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("switchState", false)
}