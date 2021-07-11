package com.sdl.atfandroid.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

class HttpRequest {
    @Nullable private String method;
    @Nullable private String path;
    @Nullable private Map<String, String> params;

    public boolean isValid() { return method != null && path != null; }

    static HttpRequest parse(String line) {
        HttpRequest request = new HttpRequest();
        try {
            // example: GET /ConnectToSDL?transport=usb&sdl_host=host_string&sdl_port=01
            String[] splitBySpace = line.split(" ");
            request.setMethod(splitBySpace[0]);

            String[] splitByQuestion = splitBySpace[1].split("\\?");
            request.setPath(splitByQuestion[0]);

            if (splitByQuestion.length > 1){
                Map<String, String> params = new HashMap<>();
                String[] splitByAnd = splitByQuestion[1].split("&");
                for (String str: splitByAnd){
                    String[] splitByEquals = str.split("=");
                    params.put(splitByEquals[0], splitByEquals[1]);
                }
                request.setParams(params);
            }

        } catch (ArrayIndexOutOfBoundsException exception){
            exception.printStackTrace();
        }

        return request;
    }

    @Nullable
    public String getMethod() {
        return method;
    }

    public void setMethod(@Nullable String method) {
        this.method = method;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @Nullable
    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(@Nullable Map<String, String> params) {
        this.params = params;
    }

    @NonNull
    @Override
    public String toString() {
        return "HttpRequestParams{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", params=" + params +
                '}';
    }
}
