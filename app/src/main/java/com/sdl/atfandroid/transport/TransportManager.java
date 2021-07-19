package com.sdl.atfandroid.transport;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.sdl.atfandroid.atf.AtfTransport;
import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.util.LogTool;
import com.sdl.atfandroid.transport.utl.TransportRecord;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class TransportManager extends TransportManagerBase {
    private static final String TAG = TransportManager.class.getSimpleName();

    final Object TRANSPORT_STATUS_LOCK;

    private volatile boolean tcpTransportAvailable = true;
    private volatile boolean bluetoothTransportAvailable = true;
    private volatile boolean usbTransportAvailable = true;


    private final HashMap<Integer, MultiplexBaseTransport> transportBySessionId = new HashMap<>();
    private final TCPHandler tcpHandler;




    public TransportManager(TransportEventListener transportListener) {
        super(transportListener);

        this.TRANSPORT_STATUS_LOCK = new Object();
        tcpHandler = new TCPHandler(this);
    }

    public boolean isTransportForSessionAvailable(TransportType type) {
        switch (type){
            case BLUETOOTH:
                return bluetoothTransportAvailable;
            case USB:
                return usbTransportAvailable;
            case TCP:
                return tcpTransportAvailable;
        }
        return false;
    }

    public void createSession(TransportType type, int sessionId, String host, int port) {
        switch (type){
            case USB:
            case BLUETOOTH:
                break;
            case TCP:
                MultiplexTcpTransport tcpTransport = new MultiplexTcpTransport(port, host, false, tcpHandler, sessionId);
                tcpTransport.start();
                transportBySessionId.put(sessionId, tcpTransport);
                tcpTransportAvailable = false;
                break;
        }
    }

    public void removeSession(int sessionId) {
        MultiplexBaseTransport transport = transportBySessionId.get(sessionId);
        if (transport != null){
            transport.stop();
            transportBySessionId.remove(sessionId);
        }
    }

    public void removeAllSessions() {
        for (Map.Entry<Integer, MultiplexBaseTransport> entry: transportBySessionId.entrySet()){
            entry.getValue().stop();
        }

        transportBySessionId.clear();
    }


    public boolean isAlive(int sessionId) {
        MultiplexBaseTransport transport = transportBySessionId.get(sessionId);
        return transport != null && transport.isConnected();
    }

    public void write(byte[] bytes, int sessionId) {
        MultiplexBaseTransport transport = transportBySessionId.get(sessionId);
        if (transport != null)
            transport.write(bytes);
    }

    protected static class TCPHandler extends Handler {

        final WeakReference<TransportManager> provider;

        public TCPHandler(TransportManager provider) {
            super(Looper.myLooper());
            this.provider = new WeakReference<>(provider);
        }

        @Override
        public void handleMessage(Message msg) {
            if (this.provider.get() == null) {
                return;
            }
            TransportManager service = this.provider.get();
            if (service.transportListener == null) {
                return;
            }
            switch (msg.what) {
                case CoreRouter.MESSAGE_STATE_CHANGE:
                    TransportRecord record = (TransportRecord) msg.obj;
                    int recordSessionId = record.getSessionId();
                    TransportType recordType = record.getType();

                    switch (msg.arg1) {
                        case MultiplexBaseTransport.STATE_CONNECTED:
                            LogTool.logInfo(TAG, "STATE_CONNECTED");
                            service.transportListener.onTransportConnected(recordType, recordSessionId);
                            service.tcpTransportAvailable = false;
                            break;
                        case MultiplexBaseTransport.STATE_CONNECTING:
                            LogTool.logInfo(TAG, "STATE_CONNECTING");
                            // Currently attempting to connect - update UI?
                            break;
                        case MultiplexBaseTransport.STATE_NONE:
                            LogTool.logInfo(TAG, "STATE_NONE");
                            MultiplexBaseTransport tcpTransport = service.transportBySessionId.get(recordSessionId);
                            if (tcpTransport != null) {
                                tcpTransport.stop();
                                service.transportBySessionId.remove(recordSessionId);
                                service.tcpTransportAvailable = true;
                            }
                            // We've just lost the connection
                            service.transportListener.onTransportDisconnected("TCP transport disconnected", recordType, recordSessionId);
                            break;
                        case MultiplexBaseTransport.STATE_ERROR:
                            LogTool.logInfo(TAG, "STATE_ERROR");
                            service.transportListener.onError("TCP transport encountered an error", recordType, recordSessionId);
                            break;
                    }
                    break;

                case CoreRouter.MESSAGE_READ:
                    LogTool.logInfo(TAG, "MESSAGE_READ");
                    service.transportListener.onPacketReceived((byte[]) msg.obj, TransportType.TCP, msg.arg1);
                    break;
            }
        }
    }

}
