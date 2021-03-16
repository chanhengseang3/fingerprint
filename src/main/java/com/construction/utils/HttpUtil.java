package com.construction.utils;

import okhttp3.*;

import java.io.IOException;

public class HttpUtil {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static Response post(String url, String body) {
        try {
            RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();
            Call call = HTTP_CLIENT.newCall(request);
            return call.execute();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public static Response get(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            Call call = HTTP_CLIENT.newCall(request);
            return call.execute();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
