package com.sdl.atfandroid.transport;

import androidx.annotation.Nullable;

import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.utl.TransportRecord;

import java.util.List;

public abstract class TransportManagerBase {
    private static String TAG = TransportManagerBase.class.getSimpleName();

    final TransportEventListener transportListener;

    public TransportManagerBase(TransportEventListener transportListener) {
        this.transportListener = transportListener;
    }

    public interface TransportEventListener {
        /**
         * Called to indicate and deliver a packet received from transport
         */
        void onPacketReceived(byte[] bytes, TransportType type, int sessionId);

        /**
         * Called to indicate that transport connection was established
         */
        void onTransportConnected(TransportType type, int sessionId);

        /**
         * Called to indicate that transport was disconnected (by either side)
         */
        void onTransportDisconnected(String info, TransportType type, int sessionId);

        // Called when the transport manager experiences an unrecoverable failure
        void onError(String info, TransportType type, int sessionId);
    }
}
