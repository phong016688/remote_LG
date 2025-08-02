package com.mentos_koder.remote_lg_tv.util

import android.content.Context

fun restoreSwitchState(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(Constants.SWITCH_STATE, false)
}
