package com.sdl.atfandroid.transport.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.sdl.atfandroid.AtfApplication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

import javax.net.SocketFactory;

public class AndroidTools {

    /**
     * Checks if the usb cable is physically connected or not
     * Note: the intent here is a sticky intent so registerReceiver is actually a synchronous call and doesn't register a receiver on each call
     *
     * @return boolean value that represents whether the usb cable is physically connected or not
     */
    public static boolean isUSBCableConnected() {
        Context context = AtfApplication.getInstance().getApplicationContext();
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return false;
        }
        context.getSystemService(Context.USB_SERVICE);
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
    }

    /**
     * Checks if the phone is connected to sdl core or not
     *
     * @param manager a UsbManager instance
     * @return UsbAccessory value that represents whether the sdl accessory available or not
     */
    public static @Nullable UsbAccessory getSdlAccessory(@NonNull UsbManager manager) {
        UsbAccessory[] accessoryList = manager.getAccessoryList();
        if (accessoryList == null){
            return null;
        }
        for (UsbAccessory accessory: accessoryList){
            String manufacturer = accessory.getManufacturer();
            if (manufacturer != null && manufacturer.equalsIgnoreCase("SDL")){
                return accessory;
            }
        }
        return null;
    }

    /**
     * Checks if the phone is connected to sdl core or not
     *
     * @return UsbAccessory value that represents whether the sdl accessory available or not
     */
    public static @Nullable UsbAccessory getSdlAccessory() {
        return getSdlAccessory((UsbManager) AtfApplication.getInstance().getApplicationContext().getSystemService(Context.USB_SERVICE));
    }

    /**
     * Checks if the usb cable is physically connected or not
     * Note: the intent here is a sticky intent so registerReceiver is actually a synchronous call and doesn't register a receiver on each call
     *
     * @return boolean value that represents whether the Bluetooth is is available or not
     */
    public static boolean isBluetoothActuallyAvailable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            //False positive
            return false;
        }

        // todo: Check this case
        int state = adapter.getProfileConnectionState(BluetoothProfile.A2DP);
        if (state != BluetoothAdapter.STATE_CONNECTING && state != BluetoothAdapter.STATE_CONNECTED) {
            //False positive
            return false;
        }

        return true;
    }

    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4   true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}
