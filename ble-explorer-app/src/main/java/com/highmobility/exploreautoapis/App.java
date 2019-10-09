package com.highmobility.exploreautoapis;

import android.app.Application;

import com.highmobility.hmkit.HMKit;

import timber.log.Timber;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // get logs from HMKit
        Timber.plant(new Timber.DebugTree());
    }
}
