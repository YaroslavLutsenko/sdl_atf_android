package com.sdl.atfandroid.atf;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.util.AndroidTools;
import com.sdl.atfandroid.transport.util.LogTool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AtfTransport extends AtfBaseTransport {
    private static final String TAG = AtfTransport.class.getSimpleName();

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int RECONNECT_DELAY = 5000;
    private static final int RECONNECT_RETRY_COUNT = 5;

    private final String ipAddress = AndroidTools.getIPAddress(true);
    private final int port;
    private final boolean autoReconnect;
    private ServerSocket mServerSocket = null;
    private Socket mSocket = null;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private AtfTransportThread mThread = null;
    private WriterThread writerThread;
    private int remainingRetry = RECONNECT_RETRY_COUNT;


    public AtfTransport(int port, boolean autoReconnect, Handler handler, int sessionId) {
        super(handler, sessionId);
        this.port = port;
        this.autoReconnect = autoReconnect;
        setState(STATE_NONE);
    }

    public synchronized void start() {
        if (getState() == STATE_NONE) {
            synchronized (this) {
                setState(STATE_CONNECTING);
                LogTool.logInfo(TAG,"AtfTransport: openServer request accepted. Starting transport thread");
                mThread = new AtfTransportThread();
                mThread.setDaemon(true);
                mThread.start();
            }
        } else {
            LogTool.logInfo(TAG,"AtfTransport: openServer request rejected. Another connection is not finished");
        }

    }

    public synchronized void reopen() {
        setState(STATE_ERROR);
        remainingRetry--;

        if (autoReconnect && remainingRetry >= 0){
            stop();
            start();
        } else {
            setState(STATE_NONE);
        }
    }

    @Override
    protected void stop(int state) {
        try {
            if (mThread != null){
                mThread.halt();
                mThread.interrupt();
            }

            if (writerThread != null) {
                writerThread.cancel();
                writerThread = null;
            }

            if (mInputStream != null){
                mInputStream.close();
            }
            mInputStream = null;

            if (mOutputStream != null){
                mOutputStream.close();
            }
            mOutputStream = null;

            if (mSocket != null) {
                mSocket.close();
            }
            mSocket = null;

            if (mServerSocket != null){
                mServerSocket.close();
            }
            mServerSocket = null;
        } catch (IOException e){
            LogTool.logError(TAG, "ATFTransport.disconnect: Exception during disconnect: " + e.getMessage());
        }

        setState(state);
    }

    @Override
    public void write(byte[] out) { write(out, 0, out.length);}

    @Override
    public void write(byte[] out, int offset, int count) {
        // Create temporary object
        WriterThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = writerThread;
        }
        // Perform the write unsynchronized
        r.write(out, offset, count);
    }

    private void startWriteThread() {
        if (writerThread == null) {
            writerThread = new WriterThread();
            writerThread.start();
        }
    }

    private class AtfTransportThread extends Thread {
        private Boolean isHalted = false;

        public void halt() {
            isHalted = true;
        }

        @Override
        public void run() {
            try {
                if (mServerSocket != null && !mServerSocket.isClosed()) {
                    mServerSocket.close();
                    mServerSocket = null;
                }
                mServerSocket = new ServerSocket(port,50, InetAddress.getByName(ipAddress));

                if (mSocket != null && !mSocket.isClosed()) {
                    mSocket.close();
                    mSocket = null;
                }
                synchronized (AtfTransport.this){
                    setState(STATE_LISTEN);
                }
                mSocket = mServerSocket.accept();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                startWriteThread();

                while (!isHalted){
                    synchronized (AtfTransport.this){
                        setState(STATE_CONNECTED);
                    }

                    byte[] buffer = new byte[READ_BUFFER_SIZE];
                    int bytesRead;
                    LogTool.logInfo("AtfTransport.run: Waiting for data...");

                    try {
                        bytesRead = mInputStream.read(buffer);
                    } catch (IOException e) {
                        LogTool.logInfo(TAG, e.getMessage());
                        reopen();
                        break;
                    }

                    if (bytesRead == -1) {
                        // Javadoc says -1 indicates end of input stream.  In TCP case this means loss
                        // of connection from HU (no exception is thrown when HU connection is lost).
//                        reopen();
                        LogTool.logInfo(TAG,"AtfTransport.run: End of connection");
                        break;
                    }

                    synchronized (AtfTransport.this) {
                        if (mThread.isInterrupted()) {
                            LogTool.logInfo(TAG,"AtfTransport.run: Got new data but thread is interrupted");
                            break;
                        }
                    }

                    synchronized (AtfTransport.this) {
                        byte[] data = Arrays.copyOf(buffer, bytesRead);
                        LogTool.logInfo(TAG, "AtfTransport.sendBytesOverTransport: successfully got data");
                        handler.obtainMessage(CoreRouter.MESSAGE_READ, sessionId, 0, data).sendToTarget();
                    }

                }

                if (mInputStream != null){
                    mInputStream.close();
                }
                mInputStream = null;

                if (mOutputStream != null){
                    mOutputStream.close();
                }
                mOutputStream = null;

            } catch (IOException ioException) {
                ioException.printStackTrace();
                reopen();
            }
        }
    }

    private class WriterThread extends Thread {
        private boolean isHalted = false;
        final BlockingQueue<OutPacket> packetQueue = new LinkedBlockingQueue<>();

        @Override
        public void run() {
            while (!isHalted) {
                try {
                    OutPacket packet = packetQueue.take();
                    if (packet == null) {
                        continue;
                    }

                    OutputStream out;
                    synchronized (AtfTransport.this) {
                        out = mOutputStream;
                    }

                    if ((out != null) && (!isHalted)) {
                        try {
                            out.write(packet.bytes, packet.offset, packet.count);
                            LogTool.logInfo(TAG,"ATFTransport.sendBytesOverTransport: successfully sent data");
                        } catch (IOException e) {
                            LogTool.logError(TAG, "ATFTransport.sendBytesOverTransport: error during sending data: " + e.getMessage());
                        }
                    } else {
                        if (isHalted) {
                            LogTool.logError(TAG, "ATFTransport: sendBytesOverTransport request accepted, thread is cancelled");
                        } else {
                            LogTool.logError(TAG, "ATFTransport: sendBytesOverTransport request accepted, but output stream is null");
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        public void write(byte[] msgBytes, int offset, int count) {
            if ((msgBytes == null) || (msgBytes.length == 0)) {
                LogTool.logInfo("ATFTransport.sendBytesOverTransport: nothing to send");
                return;
            }

            if (offset + count > msgBytes.length) {
                count = msgBytes.length - offset;
            }
            packetQueue.add(new OutPacket(msgBytes, offset, count));

        }

        public synchronized void cancel() {
            isHalted = true;
            if (mOutputStream != null) {
                synchronized (AtfTransport.this) {
                    try {
                        mOutputStream.flush();
                    } catch (IOException e) {
                        LogTool.logError(TAG, "ATFTransport flushing output stream failed: " + e.getMessage());
                    }

                    try {
                        mOutputStream.close();
                    } catch (IOException e) {
                        LogTool.logError(TAG, "ATFTransport closing output stream failed: " + e.getMessage());
                    }
                    mOutputStream = null;
                }
            }
        }
    }

    private static final class OutPacket {
        final byte[] bytes;
        final int count;
        final int offset;

        OutPacket(byte[] bytes, int offset, int count) {
            this.bytes = bytes;
            this.offset = offset;
            this.count = count;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (byte b: bytes){
                builder.append(b);
                builder.append(",");
            }
            builder.append(" count: ");
            builder.append(count);
            builder.append(" offset: ");
            builder.append(offset);
            return builder.toString();
        }
    }
}
