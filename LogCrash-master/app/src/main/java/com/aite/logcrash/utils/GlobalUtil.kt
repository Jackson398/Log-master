package com.aite.logcrash.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.aite.logcrash.LogCrashApplication

fun isDebug(context: Context): Boolean = try {
    (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
} catch (exception: Exception) {
    false
}

fun logger(message: String) {
    val context = LogCrashApplication.getContext()
    if (isDebug(context)) {
        Log.d("LogCrash", message)
    }
}