package com.sdl.atfandroid.http.data;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpConnectionInfoData implements HttpData {
    public final boolean success;
    public final boolean alive;

    public HttpConnectionInfoData(boolean success, boolean alive) {
        this.success = success;
        this.alive = alive;
    }


    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();

        try {
            object.put("success", success);
            object.put("alive", alive);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }
}
