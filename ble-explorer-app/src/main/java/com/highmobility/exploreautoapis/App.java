package com.highmobility.exploreautoapis;

import android.app.Application;

import com.highmobility.hmkit.HMKit;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        HMKit.getInstance().initialise(this);
    }
}
