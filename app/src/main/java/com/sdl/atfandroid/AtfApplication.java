package com.sdl.atfandroid;

import android.app.Application;

import com.sdl.atfandroid.util.LogTool;

public class AtfApplication extends Application {
    private static AtfApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        LogTool.deleteLogFile();
        LogTool.isLogToActivityEnabled = true;
        LogTool.isLogToFileEnabled = true;
    }

    public static AtfApplication getInstance() {
        return instance;
    }




}