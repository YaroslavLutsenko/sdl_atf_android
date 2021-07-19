package com.sdl.atfandroid.core.monitor;

public class OperationMonitor {
    public static final int WAITING_STATE = 0x1;
    public static final int FINISHED_STATE = 0xFF;

    private int state;
    private boolean transportResult;
    private boolean atfResult;
    private boolean isTransportReported;
    private boolean isAtfReported;

    public void handleResult(boolean result, Reporter reporter) {
        if (state != WAITING_STATE) { return; }

        synchronized (this){
            saveReport(result, reporter);
            state = isAtfReported && isTransportReported ? FINISHED_STATE : WAITING_STATE;
        }
    }

    private void saveReport(boolean result, Reporter reporter) {
        switch (reporter){
            case ATF:
                atfResult = result;
                isAtfReported = true;
                break;
            case SDL:
                transportResult = result;
                isTransportReported = true;
        }
    }

    public void reset() {
        state = WAITING_STATE;
        transportResult = false;
        atfResult = false;
        isAtfReported = false;
        isTransportReported = false;
    }

    public boolean getTransportResult() {
        return transportResult;
    }

    public boolean getAtfResult() {
        return atfResult;
    }

    public int getState() {
        return state;
    }
}
