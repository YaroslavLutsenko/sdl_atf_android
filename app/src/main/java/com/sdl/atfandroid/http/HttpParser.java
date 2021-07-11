package com.sdl.atfandroid.http;

import java.util.HashMap;
import java.util.Map;

public class HttpParser {

    static HttpRequest parseLine(String line) {
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

}
