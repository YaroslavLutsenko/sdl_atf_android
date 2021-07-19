package com.sdl.atfandroid.transport;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.utl.TransportRecord;

public abstract class MultiplexBaseTransport {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;    // we're doing nothing
    public static final int STATE_LISTEN = 1;    // now listening for incoming connections
    public static final int STATE_CONNECTING = 2;    // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;    // now connected to a remote device
    public static final int STATE_ERROR = 4;    // Something bad happened, we wil not try to restart the thread

    public static final String ERROR_REASON_KEY = "ERROR_REASON";
    public static final byte REASON_SPP_ERROR = 0x01;    // REASON = SPP error, which is sent through bundle.
    public static final byte REASON_NONE = 0x0;

    public static final String LOG = "log";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";

    protected int mState = STATE_NONE;
    protected final Handler handler;
    protected final TransportType transportType;

    protected TransportRecord transportRecord;
    protected String connectedDeviceName = null;
    public String connectedDeviceAddress = null;

    protected int sessionId;

    protected MultiplexBaseTransport(Handler handler, TransportType transportType, int sessionId) {
        this.handler = handler;
        this.transportType = transportType;
        this.sessionId = sessionId;
    }

    protected synchronized void setState(int state) {
        setState(state, null);
    }

    protected synchronized void setState(int state, Bundle bundle) {
        if (state == mState) {
            return; //State hasn't changed. Will not updated listeners.
        }
        //Log.d(TAG, "Setting state from: " +mState + " to: " +state);
        int arg2 = mState;
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //Also sending the previous state so we know if we lost a connection
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_STATE_CHANGE, state, arg2, getTransportRecord());
        msg.setData(bundle);
        msg.sendToTarget();
    }

    public String getAddress() {
        return connectedDeviceAddress;
    }

    public String getDeviceName() {
        return connectedDeviceName;
    }

    /**
     * Should only be called after a connection has been established
     *
     * @return TransportRecord
     */
    public TransportRecord getTransportRecord() {
        if (transportRecord == null) {
            transportRecord = new TransportRecord(transportType, connectedDeviceAddress, sessionId);
        }
        return transportRecord;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    public boolean isConnected() {
        return (mState == STATE_CONNECTED);
    }

    public synchronized void stop() {
        stop(STATE_NONE);
    }

    protected abstract void stop(int state);

    protected void stop(int state, byte error) {
    }

    public abstract void write(byte[] out);

    public abstract void write(byte[] out, int offset, int count);

}
