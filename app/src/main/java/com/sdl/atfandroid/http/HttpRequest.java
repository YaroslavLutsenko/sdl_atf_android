package com.sdl.atfandroid.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

class HttpRequest {
    @Nullable private String method;
    @Nullable private String path;
    @Nullable private Map<String, String> params;

    public boolean isValid() { return method != null && path != null; }

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
