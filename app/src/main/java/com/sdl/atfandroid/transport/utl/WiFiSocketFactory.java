package com.sdl.atfandroid.transport.utl;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import com.sdl.atfandroid.AtfApplication;
import com.sdl.atfandroid.util.LogTool;

import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

public class WiFiSocketFactory {
    private static final String TAG = WiFiSocketFactory.class.getSimpleName();

    /**
     * Try to create a TCP socket which is bound to Wi-Fi network (for Android 5+)
     * <p>
     * On Android 5 and later, this method tries to create a Socket instance which is bound to a
     * Wi-Fi network. If the phone is not connected to a Wi-Fi network, or the app lacks
     * required permission (ACCESS_NETWORK_STATE), then this method simply creates a Socket instance
     * with "new Socket();".
     *
     * @return a Socket instance, preferably bound to a Wi-Fi network
     */
    public static Socket createSocket() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Socket socket = createWiFiSocket(AtfApplication.getInstance().getApplicationContext());
            if (socket != null) {
                LogTool.logInfo(TAG, "Created a Socket bound to Wi-Fi network");
                return socket;
            }
        }

        return new Socket();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static Socket createWiFiSocket(Context context) {
        if (context == null) {
            LogTool.logInfo(TAG,"Context supplied was null");
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            LogTool.logInfo(TAG,"PackageManager isn't available.");
            return null;
        }
        // getAllNetworks() and getNetworkCapabilities() require ACCESS_NETWORK_STATE
        if (pm.checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, context.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            LogTool.logInfo(TAG,"Doesn't have ACCESS_NETWORK_STATE permission. It cannot bind a TCP transport to Wi-Fi network.");
            return null;
        }

        ConnectivityManager connMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMan == null) {
            LogTool.logInfo(TAG,"ConnectivityManager isn't available.");
            return null;
        }

        Network[] allNetworks = connMan.getAllNetworks();
        if (allNetworks == null) {
            LogTool.logInfo(TAG,"Failed to acquire a list of networks.");
            return null;
        }

        // Samsung Galaxy S9 (with Android 8.0.0) provides two `Network` instances which have
        // TRANSPORT_WIFI capability. The first one throws an IOException upon creating a Socket,
        // and the second one actually works. To support such case, here we iterate over all
        // `Network` instances until we can create a Socket.
        for (Network network : allNetworks) {
            if (network == null) {
                continue;
            }

            NetworkCapabilities capabilities = connMan.getNetworkCapabilities(network);
            if (capabilities == null) {
                continue;
            }

            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                try {
                    SocketFactory factory = network.getSocketFactory();
                    if (factory != null) {
                        return factory.createSocket();
                    }
                } catch (IOException e) {
                    LogTool.logInfo(TAG,"IOException during socket creation (ignored): " + e.getMessage());
                }
            }
        }

        LogTool.logInfo(TAG,"Cannot find Wi-Fi network to bind a TCP transport.");
        return null;
    }
}
