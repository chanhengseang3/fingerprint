package com.construction.service;

import com.construction.db.SubConstructor;
import com.construction.utils.HttpUtil;
import com.construction.utils.PropertyUtil;
import com.google.gson.Gson;
import okhttp3.Response;

public class ServerSyncService {

    private static final Gson GSON = new Gson();
    private static final String REGISTER_URL_KEY = "register.url";

    public static boolean sentToRemoteServer(SubConstructor subConstructor) {
        String url = PropertyUtil.getProperty(REGISTER_URL_KEY);
        System.out.println("url is: " + url);
        String body = GSON.toJson(subConstructor);
        Response response = HttpUtil.post(url, body);
        return response.isSuccessful();
    }
}
