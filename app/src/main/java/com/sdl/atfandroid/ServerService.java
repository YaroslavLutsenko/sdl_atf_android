package com.sdl.atfandroid;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.sdl.atfandroid.http.HttpServer;
import com.sdl.atfandroid.transport.util.AndroidTools;

public class ServerService extends Service {
    private static final String TAG = "HttpServer";
    private static final String APP_ID = "8678309";
    private static final int FOREGROUND_HTTP_SERVICE_ID = 111;

    private static HttpServer server = null;

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        Log.w(TAG, "creating HttpServer");
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterForeground();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startServer();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "destroying HttpServer");
        stopServer();
        super.onDestroy();
    }

    private void startServer() {
        if (server == null){
            server = new HttpServer();
            server.setName("HttpServer");
            server.start();
            Log.w(TAG, "started HttpServer thread");
        }
    }

    private void stopServer(){
        if (server != null){
            server.halt();
            server.interrupt();
            server = null;
        }
    }



    // Helper method to let the service enter foreground mode
    @SuppressLint("NewApi")
    private void enterForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(APP_ID, TAG, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Notification serviceNotification = new Notification.Builder(this, channel.getId())
                        .setContentTitle(AndroidTools.getIPAddress(true) + ":" + "8080" + " listening...")
                        .setSmallIcon(R.mipmap.ic_sdl)
                        .build();
                startForeground(FOREGROUND_HTTP_SERVICE_ID, serviceNotification);
            }
        }
    }
}
