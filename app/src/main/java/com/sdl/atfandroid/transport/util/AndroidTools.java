package com.sdl.atfandroid.transport.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class AndroidTools {

    /**
     * Checks if the usb cable is physically connected or not
     * Note: the intent here is a sticky intent so registerReceiver is actually a synchronous call and doesn't register a receiver on each call
     *
     * @param context a context instance
     * @return boolean value that represents whether the usb cable is physically connected or not
     */
    public static boolean isUSBCableConnected(Context context) {
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
