package com.sdl.atfandroid.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sdl.atfandroid.AtfApplication;
import com.sdl.atfandroid.R;
import com.sdl.atfandroid.http.ServerService;
import com.sdl.atfandroid.util.LogTool;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ACTION = "service-message";
    private static final String ACTION_TEXT = "service-message-text";

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String msg = intent.getStringExtra(ACTION_TEXT);
            addMessage(msg);
        }
    };

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
        LogTool.logWarning(TAG, "Process: " + id);

    }

    @Override
    protected void onResume() {
        super.onResume();
        final TextView mTextStatus = (TextView) findViewById(R.id.tvLog);
        String fromFile = LogTool.readFromFile();
        mTextStatus.setText(fromFile);
        scrollToBottom();

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter(ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(messageReceiver);
    }

    private void scrollToBottom() {
        final TextView mTextStatus = (TextView) findViewById(R.id.tvLog);
        final ScrollView mScrollView = (ScrollView) findViewById(R.id.vScrollContainer);

        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(0, mTextStatus.getBottom());
            }
        });
    }

    private void addMessage(String txt) {
        TextView mTextStatus = (TextView) findViewById(R.id.tvLog);
        String message = mTextStatus.getText() + txt + "\n\n";
        mTextStatus.setText(message);
        scrollToBottom();
    }

    public static void sendMessage(String message) {
        Intent intent = new Intent(ACTION);
        intent.putExtra(ACTION_TEXT, message);
        LocalBroadcastManager.getInstance(AtfApplication.getInstance().getApplicationContext()).sendBroadcast(intent);
    }
}