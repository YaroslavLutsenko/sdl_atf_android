package com.sdl.atfandroid.http;

import com.sdl.atfandroid.core.CoreRouter;
import com.sdl.atfandroid.http.data.HttpConnectionInfoData;
import com.sdl.atfandroid.http.data.HttpCreationOperationData;
import com.sdl.atfandroid.http.data.HttpDeletionOperationData;
import com.sdl.atfandroid.http.data.HttpDeviceInfoData;
import com.sdl.atfandroid.transport.enums.TransportType;
import com.sdl.atfandroid.util.AndroidTools;

import java.util.ArrayList;
import java.util.List;

import static com.sdl.atfandroid.http.HttpResponseFactory.ResponseResult.Bad_Request;
import static com.sdl.atfandroid.http.HttpResponseFactory.ResponseResult.Not_Found;
import static com.sdl.atfandroid.http.HttpResponseFactory.ResponseResult.Server_Error;
import static com.sdl.atfandroid.http.HttpResponseFactory.ResponseResult.Success;

public class HttpRepository {
    private static final String TAG = HttpRepository.class.getSimpleName();

    private static volatile HttpRepository instance;

    public static HttpRepository getInstance() {
        HttpRepository localInstance = instance;
        if (localInstance == null) {
            synchronized (HttpRepository.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new HttpRepository();
                }
            }
        }
        return localInstance;
    }

    public synchronized String processRequest(HttpRequest request){
        if (request == null || request.getPath() == null){
            return HttpResponseFactory.createResponse(Server_Error, null, request);
        }

        switch (request.getPath()){
            case "/GetDeviceInfo":
                return getDeviceInfo();
            case "/ConnectToSDL":
                return connectToSdl(request);
            case "/DisconnectFromSDL":
                return disconnectFromSDL(request);
            case "/GetSDLConnectionStatus":
                return getSDLConnectionStatus(request);
            default:
                return HttpResponseFactory.createResponse(Not_Found, null, request);
        }
    }

    private String getDeviceInfo() {
        boolean isUsbAvailable = AndroidTools.isUSBCableConnected() && AndroidTools.getSdlAccessory() != null;
        boolean isBluetoothAvailable = AndroidTools.isBluetoothActuallyAvailable();
        List<String> transports = new ArrayList<>();
        if (isBluetoothAvailable) transports.add("BT");
        if (isUsbAvailable) transports.add("USB");
        HttpDeviceInfoData deviceInfo = new HttpDeviceInfoData("Android", true, transports);
        return HttpResponseFactory.createResponse(Success, deviceInfo.toJSONObject(), null);
    }

    private String connectToSdl(HttpRequest request) {
        if (request.getParams() == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        String transport = request.getParams().get("transport");
        String host = request.getParams().get("sdl_host");
        String port = request.getParams().get("sdl_port");

        if (transport == null || host == null || port == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        int iPort;
        try {
            iPort = Integer.parseInt(port);
        } catch (NumberFormatException ex){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        TransportType transportType = TransportType.valueForString(transport);
        if (transportType == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        Integer sessionId = CoreRouter.instance.createSession(transportType, host, iPort);
        if (sessionId == null){
            return HttpResponseFactory.createResponse(Success, new HttpCreationOperationData(false).toJSONObject(), null);
        } else {
            return HttpResponseFactory.createResponse(Success, new HttpCreationOperationData(true, sessionId).toJSONObject(), null);
        }
    }

    private String disconnectFromSDL(HttpRequest request) {
        if (request.getParams() == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        String id = request.getParams().get("id");

        if (id == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        int sessionId;
        try {
            sessionId = Integer.parseInt(id);
        } catch (NumberFormatException ex){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        boolean result = CoreRouter.instance.deleteSession(sessionId);
        HttpDeletionOperationData data = new HttpDeletionOperationData(result);
        return HttpResponseFactory.createResponse(Success, data.toJSONObject(), null);
    }

    private String getSDLConnectionStatus(HttpRequest request) {
        if (request.getParams() == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        String id = request.getParams().get("id");

        if (id == null){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        int sessionId;
        try {
            sessionId = Integer.parseInt(id);
        } catch (NumberFormatException ex){
            return HttpResponseFactory.createResponse(Bad_Request, null, request);
        }

        boolean result = CoreRouter.instance.isConnectionAlive(sessionId);
        HttpConnectionInfoData data = new HttpConnectionInfoData(true, result);
        return HttpResponseFactory.createResponse(Success, data.toJSONObject(), null);
    }

}
