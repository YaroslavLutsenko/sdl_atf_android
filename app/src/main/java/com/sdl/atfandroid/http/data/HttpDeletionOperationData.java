package com.sdl.atfandroid.http.data;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpDeletionOperationData implements HttpData {
    public final boolean success;

    public HttpDeletionOperationData(boolean success) {
        this.success = success;
    }

    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("success", success);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

}
