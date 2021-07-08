package com.sdl.atfandroid.transport.util;

import android.util.Log;

public class LogTool {
    private static final String TAG = "LogTool";
    static private boolean logToSystemEnabled = true;
    private static final int ChunkSize = 4000;

    public enum LogTarget {
        Info, Warning, Error;

        public static LogTarget valueForString(String value) {
            try {
                return valueOf(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static void setEnableState(boolean en) {
        logToSystemEnabled = en;
    } // end-method

    public static boolean isEnabled() {
        return logToSystemEnabled;
    } // end-method

    public static boolean logInfo(String message) {
        return logInfo(TAG, message);
    }

    public static boolean logInfo(String tag, String message) {
        if (logToSystemEnabled) {
            return log(LogTarget.Info, tag, message);
        }
        return false;
    }

    public static boolean logWarning(String message) {
        return logWarning(TAG, message);
    }

    public static boolean logWarning(String tag, String message) {
        if (logToSystemEnabled) {
            return log(LogTarget.Warning, tag, message);
        }
        return false;
    }

    public static boolean logError(String message) {
        return logError(TAG, message);
    }

    public static boolean logError(String tag, String message) {
        if (logToSystemEnabled) {
            return log(LogTarget.Error, tag, message);
        }
        return false;
    }

    public static boolean logError(String message, Throwable t) {
        return logError(TAG, message, t);
    }

    public static boolean logError(String tag, String message, Throwable t) {
        // If the call to logError is passed a throwable, write directly to the system log
        if (logToSystemEnabled) {
            Log.e(tag, message, t);
        }
        return logToSystemEnabled;
    }

    private static boolean log(LogTarget logTarget, String source, String logMsg) {
        // Don't log empty messages
        if (logMsg == null || logMsg.length() == 0) {
            return false;
        }

        int subStrSize = 0;
        String chunk;
        try {
            for (int idx = 0; idx < logMsg.length(); idx += subStrSize) {
                subStrSize = Math.min(ChunkSize, logMsg.length() - idx);
                chunk = logMsg.substring(idx, idx + subStrSize);
                switch (logTarget) {
                    case Info:
                        Log.i(source, chunk);
                        break;
                    case Warning:
                        Log.w(source, chunk);
                        break;
                    case Error:
                        Log.e(source, chunk, null);
                        break;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failure writing " + logTarget.name() + " fragments to android log:" + ex.toString(), null);
            return false;
        }
        return true;
    }
}
