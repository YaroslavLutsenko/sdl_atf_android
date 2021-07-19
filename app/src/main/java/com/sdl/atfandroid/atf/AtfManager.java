package com.sdl.atfandroid.atf;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.util.LogTool;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class AtfManager {
    public final static String TAG = AtfManager.class.getSimpleName();

    private final HashMap<Integer, AtfTransport> transportBySessionId = new HashMap<>();
    private final ATFHandler atfHandler;

    private final EventListener eventListener;

    public AtfManager(EventListener eventListener) {
        this.eventListener = eventListener;
        this.atfHandler = new ATFHandler(this);
    }

    public void createSession(int sessionId) {
        if (transportBySessionId.containsKey(sessionId)) { return; }

        AtfTransport atfTransport = new AtfTransport(sessionId + 8080, true, atfHandler, sessionId);
        atfTransport.start();
        transportBySessionId.put(sessionId, atfTransport);
    }

    public void removeSession(int sessionId) {
        AtfTransport transport = transportBySessionId.get(sessionId);
        if (transport != null) {
            transport.stop();
            transportBySessionId.remove(sessionId);
        }
    }

    public void removeAllSessions() {
        for (Map.Entry<Integer, AtfTransport> entry: transportBySessionId.entrySet()){
            entry.getValue().stop();
        }

        transportBySessionId.clear();
    }

    public boolean isAlive(int sessionId) {
        AtfTransport transport = transportBySessionId.get(sessionId);
        return transport != null && transport.isAlive();
    }

    public void write(byte[] bytes, int sessionId) {
        AtfTransport transport = transportBySessionId.get(sessionId);
        if (transport != null) {
            transport.write(bytes);
        }
    }

    protected static class ATFHandler extends Handler {

        final WeakReference<AtfManager> provider;

        public ATFHandler(AtfManager provider) {
            super(Looper.myLooper());
            this.provider = new WeakReference<>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            AtfManager service = this.provider.get();
            if (service.eventListener == null) {
                return;
            }
            switch (msg.what) {
                case CoreRouter.MESSAGE_STATE_CHANGE:
                    int sessionId = (int) msg.obj;

                    switch (msg.arg1) {
                        case AtfTransport.STATE_LISTEN:
                            LogTool.logInfo(TAG, "STATE_CONNECTED");
                            service.eventListener.onAtfConnected(sessionId);
                            break;
                        case AtfTransport.STATE_CONNECTING:
                            LogTool.logInfo(TAG, "STATE_CONNECTING");
                            // Currently attempting to connect - update UI?
                            break;
                        case AtfTransport.STATE_NONE:
                            LogTool.logInfo(TAG, "STATE_NONE");
                            AtfTransport atfTransport = service.transportBySessionId.get(sessionId);
                            if (atfTransport != null) {
                                atfTransport.stop();
                                service.transportBySessionId.remove(sessionId);
                            }
                            // We've just lost the connection
                            service.eventListener.onAtfDisconnected("TCP transport disconnected", sessionId);
                            break;
                        case AtfTransport.STATE_ERROR:
                            LogTool.logInfo(TAG, "STATE_ERROR");
                            break;
                    }
                    break;

                case CoreRouter.MESSAGE_READ:
                    LogTool.logInfo(TAG, "MESSAGE_READ");
                    service.eventListener.onPacketReceived((byte[]) msg.obj, msg.arg1);
                    break;
            }
        }
    }

    public interface EventListener {
        /**
         * Called to indicate and deliver a packet received from atf
         */
        void onPacketReceived(byte[] bytes, int sessionId);

        /**
         * Called to indicate that atf connection was established
         */
        void onAtfConnected(int sessionId);

        /**
         * Called to indicate that atf was disconnected (by either side)
         */
        void onAtfDisconnected(String info, int sessionId);
    }

}
