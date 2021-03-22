package com.aite.logcrash;

import android.app.Application;
import android.content.Context;

import com.aite.logcrash.logcat.LogcatCrash;

public class LogCrashApplication extends Application {
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        LogcatCrash.Companion.initCrash(sContext, true);
    }

    public static Context getContext() {
        return sContext;
    }
}
