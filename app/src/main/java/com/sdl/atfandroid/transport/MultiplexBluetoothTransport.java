package com.sdl.atfandroid.transport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.RequiresPermission;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.util.LogTool;
import com.sdl.atfandroid.transport.utl.TransportRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 *
 * @author Joey Grover
 */
public class MultiplexBluetoothTransport extends MultiplexBaseTransport {
    //finals
    private static final String TAG = "Bluetooth Transport";
    private static final UUID SERVER_UUID = new UUID(0x936DA01F9ABD4D9DL, 0x80C702AF85C822A8L);
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = " SdlRouterService";
    // Key names received from the BluetoothSerialServer Handler
    private static final int READ_BUFFER_SIZE = 4096;
    private final Object THREAD_LOCK = new Object();

    // Member fields
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private AcceptThread mSecureAcceptThread;
    private ConnectedThread mConnectedThread;
    private ConnectedWriteThread mConnectedWriteThread;
    boolean keepSocketAlive = true;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    public MultiplexBluetoothTransport(Handler handler) {
        super(handler, TransportType.BLUETOOTH, 0);
    }

    //These methods are used so we can have a semi-static reference to the Accept Thread (Static reference inherited by housing class)
    private synchronized AcceptThread getAcceptThread() {
        return mSecureAcceptThread;
    }

    private synchronized void setAcceptThread(AcceptThread aThread) {
        mSecureAcceptThread = aThread;
    }

    protected synchronized void setStateManually(int state) {
        //Log.d(TAG, "Setting state from: " +mState + " to: " +state);
        mState = state;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    public void setKeepSocketAlive(boolean keepSocketAlive) {
        this.keepSocketAlive = keepSocketAlive;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode.
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public synchronized void start() {
        LogTool.logWarning(TAG, "Starting up Bluetooth Server to Listen");

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mConnectedWriteThread != null) {
            mConnectedWriteThread.cancel();
            mConnectedWriteThread = null;
        }


        // Start the thread to listen on a BluetoothServerSocket
        if (getAcceptThread() == null
                && mAdapter != null
                && mAdapter.isEnabled()) {
            //Log.d(TAG, "Secure thread was null, attempting to create new");
            setAcceptThread(new AcceptThread(true));
            if (getAcceptThread() != null) {
                setState(STATE_LISTEN);
                getAcceptThread().start();
            }
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH)
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mConnectedWriteThread != null) {
            mConnectedWriteThread.cancel();
            mConnectedWriteThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (!keepSocketAlive && mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Start the thread to manage the connection and perform transmissions
        mConnectedWriteThread = new ConnectedWriteThread(socket);
        mConnectedWriteThread.start();

        //Store a static name of the device that is connected.
        if (device != null) {
            connectedDeviceName = device.getName();
            connectedDeviceAddress = device.getAddress();
            if (connectedDeviceAddress != null) {
                //Update the transport record with the address
                transportRecord = new TransportRecord(transportType, connectedDeviceAddress, 0);
            }
        }

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        if (connectedDeviceName != null) {
            bundle.putString(DEVICE_NAME, connectedDeviceName);
            bundle.putString(DEVICE_ADDRESS, connectedDeviceAddress);
        }
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        stop(STATE_NONE, REASON_NONE);
    }

    protected synchronized void stop(int stateToTransitionTo) {
        this.stop(stateToTransitionTo, REASON_NONE);
    }

    @Override
    protected synchronized void stop(int stateToTransitionTo, byte error) {
        super.stop(stateToTransitionTo, error);
        LogTool.logInfo(TAG, "Attempting to close the bluetooth serial server");

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mConnectedWriteThread != null) {
            mConnectedWriteThread.cancel();
            mConnectedWriteThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (stateToTransitionTo == MultiplexBaseTransport.STATE_ERROR) {
            Bundle bundle = new Bundle();
            bundle.putByte(ERROR_REASON_KEY, error);
            setState(stateToTransitionTo, bundle);
        } else {
            setState(stateToTransitionTo, null);
        }
    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedWriteThread#write(byte[], int, int)
     */
    public void write(byte[] out, int offset, int count) {
        // Create temporary object
        ConnectedWriteThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedWriteThread;
            //r.write(out,offset,count);
        }
        // Perform the write unsynchronized
        r.write(out, offset, count);
    }

    @Override
    public void write(byte[] out) {
        write(out, 0, out.length);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_LOG);
        Bundle bundle = new Bundle();
        bundle.putString(LOG, "Unable to connect device");
        msg.setData(bundle);
        handler.sendMessage(msg);

        // Start the service over to restart listening mode
        // BluetoothSerialServer.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_LOG);
        Bundle bundle = new Bundle();
        bundle.putString(LOG, "Device connection was lost");
        msg.setData(bundle);
        handler.sendMessage(msg);
        stop();

    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        final BluetoothServerSocket mmServerSocket;
        private final String mSocketType;

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        public AcceptThread(boolean secure) {
            synchronized (THREAD_LOCK) {
                //Log.d(TAG, "Creating an Accept Thread");
                BluetoothServerSocket tmp = null;
                mSocketType = secure ? "Secure" : "Insecure";
                // Create a new listening server socket
                try {
                    if (secure) {
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, SERVER_UUID);
                    }
                } catch (IOException e) {
                    //Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
                    MultiplexBluetoothTransport.this.stop(STATE_ERROR, REASON_SPP_ERROR);
                    //Let's try to shut down this thread
                } catch (SecurityException e2) {
                    //Log.e(TAG, "<LIVIO> Security Exception in Accept Thread - "+e2.toString());
                    interrupt();
                }
                mmServerSocket = tmp;
                //Should only log on debug
                //BluetoothSocket mySock = getBTSocket(mmServerSocket);
                //Log.d(TAG, "Accepting Connections on SDP Server Port Number: " + getChannel(mySock) + "\r\n");
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH)
        public void run() {
            synchronized (THREAD_LOCK) {
                LogTool.logInfo(TAG, "Socket Type: " + mSocketType +
                        " BEGIN mAcceptThread" + this);
                setName("AcceptThread" + mSocketType);

                BluetoothSocket socket;
                int listenAttempts = 0;

                // Listen to the server socket if we're not connected
                while (mState != STATE_CONNECTED) {
                    try {
                        if (listenAttempts >= 5) {
                            LogTool.logError(TAG, "Complete failure in attempting to listen for Bluetooth connection, erroring out.");
                            MultiplexBluetoothTransport.this.stop(STATE_ERROR, REASON_NONE);
                            return;
                        }
                        listenAttempts++;
                        LogTool.logInfo(TAG, "SDL Bluetooth Accept thread is running.");

                        // This is a blocking call and will only return on a
                        // successful connection or an exception
                        if (mmServerSocket != null) {
                            socket = mmServerSocket.accept();
                        } else {
                            LogTool.logError(TAG, "Listening Socket was null, stopping the bluetooth serial server.");
                            MultiplexBluetoothTransport.this.stop(STATE_ERROR, REASON_NONE);
                            return;
                        }
                    } catch (IOException e) {
                        LogTool.logError(TAG, "Socket Type: " + mSocketType + "accept() failed");
                        MultiplexBluetoothTransport.this.stop(STATE_ERROR, REASON_SPP_ERROR);
                        return;
                    }

                    // If a connection was accepted
                    if (socket != null) {
                        synchronized (MultiplexBluetoothTransport.this) {
                            switch (mState) {
                                case STATE_LISTEN:
                                case STATE_CONNECTING:
                                    // Situation normal. Start the connected thread.
                                    connected(socket, socket.getRemoteDevice());

                                    break;
                                case STATE_NONE:
                                case STATE_CONNECTED:
                                    // Either not ready or already connected. Terminate new socket.
                                    try {
                                        LogTool.logInfo(TAG, "Close unwanted socket");
                                        socket.close();
                                    } catch (IOException e) {
                                        LogTool.logError(TAG, "Could not close unwanted socket", e);
                                    }
                                    break;
                            }
                        }
                    }
                }
                LogTool.logInfo(TAG, mState + " END mAcceptThread, socket Type: " + mSocketType);
            }
        }

        public synchronized void cancel() {
            LogTool.logInfo(TAG, mState + " Socket Type " + mSocketType + " cancel ");
            try {
                if (mmServerSocket != null) {
                    mmServerSocket.close();
                }

            } catch (IOException e) {
                LogTool.logError(TAG, mState + " Socket Type " + mSocketType + " close() of server failed " + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedWriteThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;


        public ConnectedWriteThread(BluetoothSocket socket) {
            //Log.d(TAG, "Creating a Connected - Write Thread");
            mmSocket = socket;
            OutputStream tmpOut = null;
            setName("SDL Router BT Write Thread");
            // Get the BluetoothSocket input and output streams
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                // temp sockets not created
                LogTool.logError(TAG, "Connected Write Thread: " + e.getMessage());
            }
            mmOutStream = tmpOut;


        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer, int offset, int count) {
            try {
                if (buffer == null) {
                    LogTool.logWarning(TAG, "Can't write to device, nothing to send");
                    return;
                }
                //This would be a good spot to log out all bytes received
                StringBuilder builder = new StringBuilder("=> " + buffer.length + ": ");
                for (byte b: buffer){
                    builder.append(b);
                    builder.append(",");
                }
                LogTool.logWarning(TAG, builder.toString());
                mmOutStream.write(buffer, offset, count);
                LogTool.logWarning(TAG, "Wrote out to device: bytes = "+ count);
            } catch (IOException | NullPointerException e) { // STRICTLY to catch mmOutStream NPE
                // Exception during write
                //OMG! WE MUST NOT BE CONNECTED ANYMORE! LET THE USER KNOW
                LogTool.logError(TAG, "Error sending bytes to connected device!");
                connectionLost();
            }
        }

        public synchronized void cancel() {
            try {
                if (mmOutStream != null) {
                    mmOutStream.flush();
                    mmOutStream.close();

                }
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e) {
                // close() of connect socket failed
                LogTool.logInfo(TAG, "Write Thread: " + e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket) {
            LogTool.logWarning(TAG, "Creating a Connected - Read Thread");
            mmSocket = socket;
            InputStream tmpIn = null;
            setName("SDL Router BT Read Thread");
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                // temp sockets not created
                LogTool.logError(TAG, "Connected Read Thread: " + e.getMessage());
            }
            mmInStream = tmpIn;

        }

        @SuppressLint("NewApi")
        public void run() {
            LogTool.logInfo(TAG, "Running the Connected Thread");

            int bytesRead = 0;
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            // Keep listening to the InputStream while connected
            boolean stateProgress;

            while (true) {
                try {
                    bytesRead = mmInStream.read(buffer);
                } catch (IOException | NullPointerException e) { // NPE is ONLY to catch error on mmInStream
                    LogTool.logError(TAG, "Lost connection in the Connected Thread", e);
                    connectionLost();
                    break;
                }

                // not sure this makes sense
                if (bytesRead == -1){
                    LogTool.logError(TAG, "Lost connection in the Connected Thread");
                    connectionLost();
                    break;
                }

                synchronized (MultiplexBluetoothTransport.this) {
                    byte[] data = Arrays.copyOf(buffer, bytesRead);
                    LogTool.logInfo(TAG, "successfully got data");
                    handler.obtainMessage(CoreRouter.MESSAGE_READ, sessionId, 0, data).sendToTarget();
                }
            }
        }


        public synchronized void cancel() {
            try {
                //Log.d(TAG, "Calling Cancel in the Read thread");
                if (mmInStream != null) {
                    mmInStream.close();
                }
                if (mmSocket != null) {
                    mmSocket.close();
                }

            } catch (IOException | NullPointerException e) { // NPE is ONLY to catch error on mmInStream
                // Log.trace(TAG, "Read Thread: " + e.getMessage());
                // Socket or stream is already closed
            }
        }
    }

}
