package com.sdl.atfandroid.transport;

import android.hardware.usb.UsbAccessory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.transport.util.LogTool;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MultiplexUsbTransport extends MultiplexBaseTransport {

    private static final String TAG = "MultiplexUsbTransport";

    private ReaderThread readerThread;
    private WriterThread writerThread;
    private ParcelFileDescriptor parcelFileDescriptor;
    private Boolean connectionSuccessful = null;

    public MultiplexUsbTransport(ParcelFileDescriptor parcelFileDescriptor, Handler handler, UsbAccessory accessory) {
        super(handler, TransportType.USB, 0);
        if (parcelFileDescriptor == null || accessory == null) {
            LogTool.logError(TAG, "Error with object");
            this.parcelFileDescriptor = null;
            throw new ExceptionInInitializerError("ParcelFileDescriptor and UsbAccessory can't be null");
        } else {
            this.parcelFileDescriptor = parcelFileDescriptor;
            connectedDeviceName = "USB";
            //Fill in info
            connectedDeviceAddress = accessory.getSerial();
            if (connectedDeviceAddress == null) {
                connectedDeviceAddress = accessory.getUri();
                if (connectedDeviceAddress == null) {
                    connectedDeviceAddress = accessory.getDescription();
                    if (connectedDeviceAddress == null) {
                        connectedDeviceAddress = accessory.getModel();
                        if (connectedDeviceAddress == null) {
                            connectedDeviceAddress = accessory.getManufacturer();
                        }
                    }
                }
            } else {
                connectedDeviceAddress = "USB";
            }
        }
    }

    public synchronized void start() {
        LogTool.logWarning(TAG, "start");
        setState(STATE_CONNECTING);
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        if (fileDescriptor == null || !fileDescriptor.valid()) {
            LogTool.logError(TAG, "USB FD was null or not valid,");
            setState(STATE_NONE);
            return;
        }
        readerThread = new ReaderThread(fileDescriptor);
        readerThread.setDaemon(true);
        writerThread = new WriterThread(fileDescriptor);
        writerThread.setDaemon(true);

        readerThread.start();
        writerThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handler.obtainMessage(CoreRouter.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, connectedDeviceName);
        bundle.putString(DEVICE_ADDRESS, connectedDeviceAddress);
        msg.setData(bundle);
        handler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    protected synchronized void stop(int stateToTransitionTo) {
        LogTool.logWarning(TAG, "Attempting to close the Usb transports");
        if (writerThread != null) {
            writerThread.cancel();
            writerThread.interrupt();
            writerThread = null;
        }

        if (readerThread != null) {
            readerThread.cancel();
            readerThread.interrupt();
            readerThread = null;
        }

        if ((connectionSuccessful == null || connectionSuccessful)      //else, the connection was bad. Not closing the PFD helps recover
                && parcelFileDescriptor != null) {
            try {
                parcelFileDescriptor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        parcelFileDescriptor = null;

        System.gc();

        setState(stateToTransitionTo);
    }


    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     */
    public void write(byte[] out, int offset, int count) {
        // Create temporary object
        MultiplexUsbTransport.WriterThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = writerThread;
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
        handler.postDelayed(new Runnable() { //sends this stop back to the main thread to exit the reader thread
            @Override
            public void run() {
                stop();
            }
        }, 250);
    }

    private class ReaderThread extends Thread {
        final InputStream inputStream;

        public ReaderThread(final FileDescriptor fileDescriptor) {
            inputStream = new FileInputStream(fileDescriptor);
        }

        @Override
        public void run() {
            final int READ_BUFFER_SIZE = 16384;
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            int bytesRead;

            // read loop
            while (!isInterrupted()) {
                try {
                    bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1) {
                        if (isInterrupted()) {
                            LogTool.logError(TAG, "EOF reached, and thread is interrupted");
                        } else {
                            LogTool.logInfo(TAG, "EOF reached, disconnecting!");
                            connectionLost();
                        }
                        return;
                    }
                    if (isInterrupted()) {
                        LogTool.logWarning(TAG, "Read some data, but thread is interrupted");
                        return;
                    }
                    if (connectionSuccessful != null && !connectionSuccessful) {
                        connectionSuccessful = true;
                    }
                    synchronized (MultiplexUsbTransport.this) {
                        byte[] data = Arrays.copyOf(buffer, bytesRead);
                        StringBuilder builder = new StringBuilder("<= " + data.length + ": ");
                        for (byte b: data){
                            builder.append(b);
                            builder.append(",");
                        }
                        LogTool.logWarning(TAG, builder.toString());
                        LogTool.logInfo(TAG, "successfully got data");
                        handler.obtainMessage(CoreRouter.MESSAGE_READ, sessionId, 0, data).sendToTarget();
                    }

                } catch (IOException e) {
                    if (isInterrupted()) {
                        LogTool.logWarning(TAG, "Can't read data, and thread is interrupted");
                    } else {
                        LogTool.logWarning(TAG, "Can't read data, disconnecting!");
                        connectionLost();
                    }
                    return;
                } catch (Exception e) {
                    connectionLost();
                }
            }
        }


        public synchronized void cancel() {
            try {
                //Log.d(TAG, "Calling Cancel in the Read thread");
                if (inputStream != null) {
                    inputStream.close();
                }

            } catch (IOException | NullPointerException e) { // NPE is ONLY to catch error on mmInStream
                // Log.trace(TAG, "Read Thread: " + e.getMessage());
                // Socket or stream is already closed
            }
        }

    }


    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class WriterThread extends Thread {
        private final OutputStream mmOutStream;

        public WriterThread(FileDescriptor fileDescriptor) {
            //Log.d(TAG, "Creating a Connected - Write Thread");
            OutputStream tmpOut = null;
            setName("SDL USB Write Thread");
            // Get the Usb output streams
            mmOutStream = new FileOutputStream(fileDescriptor);
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
                if (connectionSuccessful == null) {
                    connectionSuccessful = false;
                }
                //Log.w(TAG, "Wrote out to device: bytes = "+ count);
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
            } catch (IOException e) {
                // close() of connect socket failed
                LogTool.logInfo(TAG, "Write Thread: " + e.getMessage());
            }
        }
    }
}

