package com.sdl.atfandroid.http;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

class HttpResponseFactory {


    public static String  createResponse(ResponseResult result, @Nullable JSONObject body, @Nullable HttpRequest request) {
        switch (result){
            case Success:
                return createOKResponse(body);
            case Not_Found:
                return createNotFoundErrorResponse(request);
            case Bad_Request:
                return createBadRequestErrorResponse(request);
            case Server_Error:
                return createServerErrorResponse();
        }


        return createServerErrorResponse();
    }

    private static String createOKResponse(@Nullable JSONObject body) {
        String CRLF = "\n\r";

        String localBody = (body == null ? new JSONObject() : body).toString();

        return "HTTP/1.1 200" + CRLF +
                "\n" +
                localBody +
                CRLF +
                CRLF;
    }

    private static String createNotFoundErrorResponse(@Nullable HttpRequest request) {
        String CRLF = "\n\r";

        StringBuilder builder = new StringBuilder();
        if (request != null){
            if (request.getMethod() != null)
                builder.append("method: ").append(request.getMethod());
        }

        JSONObject object = new JSONObject();
        try {
            object.put("status", ResponseResult.Not_Found.code);
            object.put("error", ResponseResult.Not_Found.text);
            object.put("message", builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = object.toString();

        return "HTTP/1.1 404 Not Found " + CRLF +
                "\n" +
                body +
                CRLF +
                CRLF;
    }

    private static String createBadRequestErrorResponse(@Nullable HttpRequest request) {
        String CRLF = "\n\r";

        StringBuilder builder = new StringBuilder();
        if (request != null){
            if (request.getMethod() != null)
                builder.append("method: ").append(request.getMethod());
            if (request.getPath() != null)
                builder.append(" path: ").append(request.getPath());
            if (request.getParams() != null)
                builder.append(" params: ").append(request.getParams());
        }

        JSONObject object = new JSONObject();
        try {
            object.put("status", ResponseResult.Bad_Request.code);
            object.put("error", ResponseResult.Bad_Request.text);
            object.put("message", builder.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = object.toString();

        return "HTTP/1.1 400 Bad Request " + CRLF +
                "\n" +
                body +
                CRLF +
                CRLF;
    }

    private static String createServerErrorResponse() {
        String CRLF = "\n\r";

        JSONObject object = new JSONObject();
        try {
            object.put("status", ResponseResult.Server_Error.code);
            object.put("error", ResponseResult.Server_Error.text);
            object.put("message", "Something go wrong");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String body = object.toString();

        return "HTTP/1.1 500 " + CRLF +
                "\n" +
                body +
                CRLF +
                CRLF;
    }

    enum ResponseResult {
        Success(200, "OK"),
        Not_Found(404, "Not Found"),
        Bad_Request(400, "Bad Request"),
        Server_Error(500, "Internal Server Error");

        private final int code;
        private final String text;

        ResponseResult(int code, String text) {
            this.code = code;
            this.text = text;
        }
    }

}
