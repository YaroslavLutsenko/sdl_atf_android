package com.sdl.atfandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.sdl.atfandroid.transport.MultiplexTcpTransport;
import com.sdl.atfandroid.transport.util.LogTool;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    MultiplexTcpTransport tcpTransport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tcpTransport = new MultiplexTcpTransport(12345, "192.168.0.104", true, new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                LogTool.logWarning(TAG, "WHAT " + msg.what);
            }
        }, this);
        tcpTransport.start();

        findViewById(R.id.tvHelloWorld).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                LogTool.logWarning(TAG, "==> start sending!");
//                byte[] send = new byte[(byte) 16];
//                tcpTransport.write(send);

                byte[] pps = new byte[]{
                        16,7,1,0,0,0,0,32,32,0,0,0,2,112,114,111,116,111,99,111,108,86,101,114,115,105,111,110,0,6,0,0,0,53,46,52,46,48,0,0
                };
                tcpTransport.write(pps, 0, pps.length);
            }
        });

    }


}