package com.sdl.atfandroid.core;

import androidx.annotation.Nullable;

import com.sdl.atfandroid.atf.AtfManager;
import com.sdl.atfandroid.atf.AtfManager.EventListener;
import com.sdl.atfandroid.core.monitor.OperationMonitor;
import com.sdl.atfandroid.transport.TransportManager;
import com.sdl.atfandroid.transport.TransportManagerBase.TransportEventListener;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.util.LogTool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;

import static com.sdl.atfandroid.core.monitor.Reporter.ATF;
import static com.sdl.atfandroid.core.monitor.Reporter.SDL;

public class CoreRouter {

    public static final String TAG = CoreRouter.class.getSimpleName();

    public static final int MESSAGE_STATE_CHANGE = 5;
    public static final int MESSAGE_READ = 6;
    public static final int MESSAGE_DEVICE_NAME = 8;
    public static final int MESSAGE_LOG = 9;

    public static final CoreRouter instance = new CoreRouter();

    private final AtomicInteger sessionIdGenerator = new AtomicInteger(0);

    // Managers
    private TransportManager transportManager;
    private AtfManager atfManager;

    // Listeners
    private TransportEventListener transportEventListener;
    private EventListener atfEventListener;

    // Monitors
    private final OperationMonitor sessionCreationMonitor = new OperationMonitor();
    private final OperationMonitor sessionDeletionMonitor = new OperationMonitor();


    public void init() {
        if (transportEventListener == null)
            createTransportListener();

        if (transportManager == null)
            transportManager = new TransportManager(transportEventListener);

        if (atfEventListener == null)
            createAtfEventListener();

        if (atfManager == null)
            atfManager = new AtfManager(atfEventListener);
    }

    public void close(){
        if (atfManager != null){
            atfManager.removeAllSessions();
            atfManager = null;
        }
        atfEventListener = null;

        if (transportManager != null){
            transportManager.removeAllSessions();
            transportManager = null;
        }
        transportEventListener = null;

        sessionCreationMonitor.reset();
        sessionDeletionMonitor.reset();

        sessionIdGenerator.set(0);
    }

    @Nullable
    public synchronized Integer createSession(TransportType type, String host, int port) {
        if (!transportManager.isTransportForSessionAvailable(type)){
            return null;
        }

        sessionCreationMonitor.reset();
        int sessionId = sessionIdGenerator.incrementAndGet();
        transportManager.createSession(type, sessionId, host, port);
        atfManager.createSession(sessionId);

        while (sessionCreationMonitor.getState() != OperationMonitor.FINISHED_STATE){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex){
                return null;
            }
        }

        return sessionCreationMonitor.getTransportResult() && sessionCreationMonitor.getAtfResult() ? sessionId : null;
    }

    public synchronized boolean deleteSession(int sessionId) {
        sessionDeletionMonitor.reset();

        boolean isAtfSessionAlive = atfManager.isAlive(sessionId);
        if (!isAtfSessionAlive) {
            sessionDeletionMonitor.handleResult(true, ATF);
        }
        atfManager.removeSession(sessionId);

        boolean isSdlSessionAlive = transportManager.isAlive(sessionId);
        if (!isSdlSessionAlive) {
            sessionDeletionMonitor.handleResult(true, SDL);
        }
        transportManager.removeSession(sessionId);

        while (sessionDeletionMonitor.getState() != OperationMonitor.FINISHED_STATE){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex){
                return false;
            }
        }

        return sessionDeletionMonitor.getTransportResult() && sessionDeletionMonitor.getAtfResult();
    }

    public synchronized Boolean isConnectionAlive(int sessionId) {
        return transportManager.isAlive(sessionId);
    }

    private void createTransportListener() {
        transportEventListener = new TransportEventListener() {
            @Override
            public void onPacketReceived(byte[] bytes, TransportType type, int sessionId) {
                LogTool.logWarning(TAG, "<= onPacketReceived transportEventListener sessionId " + sessionId + " bytes " + bytes.length);
                atfManager.write(bytes, sessionId);
            }

            @Override
            public void onTransportConnected(TransportType type, int sessionId) {
                LogTool.logWarning(TAG, "onTransportConnected " + sessionId);
                sessionCreationMonitor.handleResult(true, SDL);
                atfManager.createSession(sessionId);
            }

            @Override
            public void onTransportDisconnected(String info, TransportType type, int sessionId) {
                LogTool.logWarning(TAG, "onTransportDisconnected " + sessionId);
                sessionCreationMonitor.handleResult(false, SDL);
                sessionDeletionMonitor.handleResult(true, SDL);
                atfManager.removeSession(sessionId);
            }

            @Override
            public void onError(String info, TransportType type, int sessionId) {

            }
        };

    }

    private void createAtfEventListener() {
        atfEventListener = new EventListener() {
            @Override
            public void onPacketReceived(byte[] bytes, int sessionId) {
                LogTool.logWarning(TAG, "<= onPacketReceived atfEventListener sessionId " + sessionId + " bytes " + bytes.length);
                transportManager.write(bytes, sessionId);
            }

            @Override
            public void onAtfConnected(int sessionId) {
                LogTool.logWarning(TAG, "ATF onAtfConnected " + sessionId);
                sessionCreationMonitor.handleResult(true, ATF);
            }

            @Override
            public void onAtfDisconnected(String info, int sessionId) {
                LogTool.logWarning(TAG, "ATF onAtfDisconnected " + sessionId);
                sessionCreationMonitor.handleResult(false, ATF);
                sessionDeletionMonitor.handleResult(true, ATF);
            }
        };
    }


}
