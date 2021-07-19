package com.sdl.atfandroid;

import android.app.Application;
import android.content.Context;

public class AtfApplication extends Application {
    private static AtfApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static AtfApplication getInstance() {
        return instance;
    }




}