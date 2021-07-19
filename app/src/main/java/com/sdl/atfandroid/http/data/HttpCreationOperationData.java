package com.sdl.atfandroid.http.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sdl.atfandroid.transport.util.AndroidTools;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpCreationOperationData implements HttpData {
    public final boolean success;
    @Nullable public final Integer id;
    @Nullable public final String proxy_host;
    @Nullable public final Integer proxy_port;

    public HttpCreationOperationData(boolean success) {
        this.success = success;
        this.id = null;
        this.proxy_host = null;
        this.proxy_port = null;
    }

    public HttpCreationOperationData(boolean success, @NonNull Integer id) {
        this.success = success;
        this.id = id;
        this.proxy_host = AndroidTools.getIPAddress(true);
        this.proxy_port = id + 8080;
    }


    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("success", success);
            if (id != null) object.put("id", id);
            if (proxy_host != null) object.put("proxy_host", proxy_host);
            if (proxy_port != null) object.put("proxy_port", proxy_port);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

}
