package com.sdl.atfandroid;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.sdl.atfandroid.transport.MultiplexBluetoothTransport;
import com.sdl.atfandroid.transport.MultiplexTcpTransport;
import com.sdl.atfandroid.transport.MultiplexUsbTransport;
import com.sdl.atfandroid.transport.util.AndroidTools;
import com.sdl.atfandroid.transport.util.LogTool;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    MultiplexTcpTransport tcpTransport;
    MultiplexBluetoothTransport bluetoothTransport;
    MultiplexUsbTransport usbTransport;

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

        LogTool.logWarning(TAG, "=> onCreate ");

//        tcpTransport = new MultiplexTcpTransport(12345, "192.168.0.104", true, new Handler(Looper.myLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                LogTool.logWarning(TAG, "WHAT " + msg.what);
//            }
//        }, this);
//        tcpTransport.start();

//        bluetoothTransport = new MultiplexBluetoothTransport(new Handler(Looper.myLooper()){});

        int id = android.os.Process.myPid();
        LogTool.logWarning(TAG, "=> => => Process: " + id);

        findViewById(R.id.btnConnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbTransport != null){ return; }

                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                UsbAccessory accessory = AndroidTools.getSdlAccessory(manager);
                if (accessory != null){
                    ParcelFileDescriptor parcelFileDescriptor = manager.openAccessory(accessory);
                    usbTransport = new MultiplexUsbTransport(parcelFileDescriptor, new Handler(Looper.myLooper()){}, accessory);
                    usbTransport.start();

                    byte[] data = new byte[]{
                            16,7,1,0,0,0,0,32,32,0,0,0,2,112,114,111,116,111,99,111,108,86,101,114,115,105,111,110,0,6,0,0,0,53,46,52,46,48,0,0
                    };
                    usbTransport.write(data);
                } else {
                    LogTool.logError(TAG, "accessory list not found");
                }

//                bluetoothTransport.start();
//                LogTool.logWarning(TAG, "==> start sending!");
//                byte[] send = new byte[(byte) 16];
//                tcpTransport.write(send);

//                byte[] data = new byte[]{
//                        40: 16,7,1,0,0,0,0,32,32,0,0,0,2,112,114,111,116,111,99,111,108,86,101,114,115,105,111,110,0,6,0,0,0,53,46,52,46,48,0,0
//                };

         //       309: 80,7,2,1,0,0,1,41,0,0,0,0,41,1,0,0,4,118,105,100,101,111,83,101,114,118,105,99,101,84,114,97,110,115,112,111,114,116,115,0,12,0,0,0,16,48,0,1,0,0,0,0,16,104,97,115,104,73,100,0,13,0,1,0,2,109,111,100,101,108,0,8,0,0,0,71,101,110,101,114,105,99,0,18,109,116,117,0,0,0,2,0,0,0,0,0,2,109,111,100,101,108,89,101,97,114,0,5,0,0,0,50,48,49,57,0,2,115,121,115,116,101,109,72,97,114,100,119,97,114,101,86,101,114,115,105,111,110,0,13,0,0,0,49,50,51,46,52,53,54,46,55,56,57,48,0,2,109,97,107,101,0,4,0,0,0,83,68,76,0,4,115,101,99,111,110,100,97,114,121,84,114,97,110,115,112,111,114,116,115,0,5,0,0,0,0,2,115,121,115,116,101,109,83,111,102,116,119,97,114,101,86,101,114,115,105,111,110,0,9,0,0,0,49,50,51,52,53,95,85,83,0,2,116,114,105,109,0,3,0,0,0,83,69,0,4,97,117,100,105,111,83,101,114,118,105,99,101,84,114,97,110,115,112,111,114,116,115,0,12,0,0,0,16,48,0,1,0,0,0,0,2,112,114,111,116,111,99,111,108,86,101,114,115,105,111,110,0,6,0,0,0,53,46,52,46,48,0,0

//                bluetoothTransport.write(data);
            }
        });

        findViewById(R.id.btnDisconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usbTransport.stop();
                usbTransport = null;
//                bluetoothTransport.stop();
            }
        });

    }


}