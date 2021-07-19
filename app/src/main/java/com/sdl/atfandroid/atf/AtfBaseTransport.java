package com.sdl.atfandroid.atf;

import android.os.Handler;
import android.os.Message;

import com.sdl.atfandroid.core.CoreRouter;

public abstract class AtfBaseTransport {
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;    // we're doing nothing
    public static final int STATE_LISTEN = 1;    // now listening for incoming connections
    public static final int STATE_CONNECTING = 2;    // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;    // now connected to a remote device
    public static final int STATE_ERROR = 4;    // Something bad happened, we wil not try to restart the thread

    protected int mState = STATE_NONE;
    protected final Handler handler;
    protected final int sessionId;

    public AtfBaseTransport(Handler handler, int sessionId) {
        this.handler = handler;
        this.sessionId = sessionId;
    }

    protected synchronized void setState(int state) {
        if (state == mState) {
            return; //State hasn't changed. Will not updated listeners.
        }
        //Log.d(TAG, "Setting state from: " +mState + " to: " +state);
        int arg2 = mState;
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //Also sending the previous state so we know if we lost a connection
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_STATE_CHANGE, state, arg2, sessionId);
        msg.sendToTarget();
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

    public boolean isAlive() {
        return (mState == STATE_CONNECTED || mState == STATE_LISTEN);
    }

    public synchronized void stop() {
        stop(STATE_NONE);
    }

    protected abstract void stop(int state);

    public abstract void write(byte[] out);

    public abstract void write(byte[] out, int offset, int count);

}
