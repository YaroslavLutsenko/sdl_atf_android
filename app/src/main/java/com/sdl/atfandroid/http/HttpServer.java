package com.sdl.atfandroid.http;

import android.util.Log;

import com.sdl.atfandroid.transport.util.AndroidTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends Thread {
    private static final String TAG = HttpServer.class.getSimpleName();

    private boolean isHalted = false;
    private ServerSocket serverSocket = null;
    final Map<Long, Thread> sessionsById = new HashMap<>();

    public void halt(){
        isHalted = true;

        if (serverSocket != null){
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException ioException) {
                Log.e(TAG, "can't close server socket!", ioException);
            }
        }

        for (Map.Entry<Long, Thread> entry : sessionsById.entrySet()) {
            entry.getValue().interrupt();
        }
        sessionsById.clear();
    }

    synchronized void removeSession(Long sessionId) {
        sessionsById.remove(sessionId);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8080,50, InetAddress.getByName(AndroidTools.getIPAddress(true)));
            Log.w(TAG, "=> listening... host " + serverSocket.getInetAddress().getHostName() + " port " + serverSocket.getLocalPort());

            while (!isHalted && serverSocket != null && !serverSocket.isClosed() && serverSocket.isBound()) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(5000);

                long sessionId = System.nanoTime();
                final HttpConnectionWorkerThread thread = new HttpConnectionWorkerThread(socket, sessionId);
                thread.setName("HttpRequest thread: " + sessionId);
                thread.start();
                sessionsById.put(sessionId, thread);
                Log.w(TAG, "started new HttpRequest " + sessionId);

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        while (true){
//                            try {
//                                Thread.sleep(10000);
//
//                                Log.w(TAG, "sessions: " + sessionsById.size());
//                                for (Map.Entry<Long, Thread> entry : sessionsById.entrySet()) {
//                                    Log.w(TAG, entry.getKey() + "/" + entry.getValue());
//                                    Log.w(TAG, (entry.getValue() == null) + "/" + (entry.getValue() == null || entry.getValue().isAlive()));
//
//                                }
//
//
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }).start();
            }
        } catch (IOException exception){
            Log.e(TAG, "can't accept socket to server!", exception);
        }
    }

    private class HttpConnectionWorkerThread extends Thread {
        private final Socket clientSocket;
        private final Long sessionId;

        public HttpConnectionWorkerThread(Socket clientSocket, Long sessionId) {
            this.clientSocket = clientSocket;
            this.sessionId = sessionId;
        }

        @Override
        public void run() {
            BufferedReader reader;
            PrintWriter writer;
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                writer = new PrintWriter(clientSocket.getOutputStream(), true);
                HttpRequest request = HttpRequest.parse(reader.readLine());

                String response = HttpRepository.getInstance().processRequest(request);
                writer.print(response);
                writer.flush();

                reader.close();
                writer.close();
                clientSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "error while working with socket", ioException);
            }

            Log.w(TAG, "end working with socket");
            removeSession(sessionId);
        }
    }
}
