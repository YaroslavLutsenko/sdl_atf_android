package com.sdl.atfandroid.view;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import androidx.appcompat.app.AppCompatActivity;

import com.sdl.atfandroid.R;
import com.sdl.atfandroid.ServerService;
import com.sdl.atfandroid.transport.util.LogTool;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, ServerService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        int id = Process.myPid();
        LogTool.logWarning(TAG, "=> => => Process: " + id);

    }


}