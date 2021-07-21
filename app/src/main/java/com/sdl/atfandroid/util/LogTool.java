package com.sdl.atfandroid.util;

import android.content.Context;
import android.util.Log;

import com.sdl.atfandroid.AtfApplication;
import com.sdl.atfandroid.view.MainActivity;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LogTool {
    private static final String TAG = LogTool.class.getSimpleName();

    public static boolean isLogToFileEnabled = false;
    public static boolean isLogToActivityEnabled = false;


    public enum LogTarget {
        Info, Warning, Error
    }

    public static void logToActivity(String message) {
        MainActivity.sendMessage(message);
    }

    public static void logInfo(String message) {
        logInfo(TAG, message);
    }

    public static void logInfo(String tag, String message) {
        log(LogTarget.Info, tag, message);
    }

    public static void logWarning(String message) {
        logWarning(TAG, message);
    }

    public static void logWarning(String tag, String message) {
        log(LogTarget.Warning, tag, message);
    }

    public static void logError(String message) {
        logError(TAG, message);
    }

    public static void logError(String tag, String message) {
        log(LogTarget.Error, tag, message);
    }

    public static void logError(String message, Throwable t) {
        logError(TAG, message, t);
    }

    public static void logError(String tag, String message, Throwable t) {
        Log.e(tag, message, t);
    }

    private static synchronized void logToFile(String source, String logMsg) {
        try {
            String msg = source + ": " + logMsg + "\n";
            FileOutputStream fos = AtfApplication.getInstance().openFileOutput("log.txt", Context.MODE_APPEND);
            fos.write(msg.getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static synchronized void logToFile(String source, byte[] bytes) {
        try {
            String msg = source + ": ";
            FileOutputStream fos = AtfApplication.getInstance().openFileOutput("log.txt", Context.MODE_APPEND);
            fos.write(msg.getBytes());
            fos.write(bytes);
            fos.write("\n".getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static synchronized String readFromFile() {
        String ret = "";

        try {
            InputStream inputStream = AtfApplication.getInstance().getApplicationContext().openFileInput("log.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static void deleteLogFile(){
        AtfApplication.getInstance().getApplicationContext().deleteFile("log.txt");
    }

    private static void log(LogTarget logTarget, String source, String logMsg) {
        if (logMsg == null || logMsg.length() == 0) {
            return;
        }

        switch (logTarget) {
            case Info:
                Log.i(source, logMsg);
                break;
            case Warning:
                Log.w(source, logMsg);
                break;
            case Error:
                Log.e(source, logMsg, null);
                break;
        }

        if (isLogToFileEnabled)
            logToFile(source, logMsg);

        if (isLogToActivityEnabled)
            logToActivity(logMsg);
    }
}
