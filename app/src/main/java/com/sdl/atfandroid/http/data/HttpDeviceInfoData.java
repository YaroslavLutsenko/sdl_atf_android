package com.sdl.atfandroid.http.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class HttpDeviceInfoData implements HttpData {
    public final String os;
    public final boolean success;
    public final List<String> transports;

    public HttpDeviceInfoData(String os, boolean success, List<String> transports) {
        this.os = os;
        this.success = success;
        this.transports = transports;
    }


    public JSONObject toJSONObject() {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for (String tr: transports)
            array.put(tr);

        try {
            object.put("os", os);
            object.put("transports", array);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }
}
