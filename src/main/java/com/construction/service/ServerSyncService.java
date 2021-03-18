package com.construction.service;

import com.construction.db.DbConnector;
import com.construction.db.SubConstructor;
import com.construction.utils.HttpUtil;
import com.construction.utils.PropertyUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

public class ServerSyncService {

    private static final DbConnector DB = new DbConnector();
    private static final Gson GSON = new Gson();
    private static final String REGISTER_URL_KEY = "register.url";
    private static final String GET_ALL_KEY = "get.all.url";

    public static int sentToRemoteServer(SubConstructor subConstructor) {
        String url = PropertyUtil.getProperty(REGISTER_URL_KEY);
        String body = GSON.toJson(subConstructor);
        Response response = HttpUtil.post(url, body);
        return response.code();
    }

    public static boolean syncFromServer() {
        try {
            String url = PropertyUtil.getProperty(GET_ALL_KEY);
            Response response = HttpUtil.get(url);
            if (200 != response.code()) {
                return false;
            }
            String result = Objects.requireNonNull(response.body()).string();
            Type typeToken = new TypeToken<List<SubConstructor>>() {
            }.getType();
            List<SubConstructor> subConstructors = GSON.fromJson(result, typeToken);
            subConstructors.stream()
                    .filter(subConstructor -> subConstructor.getBase64() != null)
                    .filter(subConstructor -> !subConstructor.getBase64().isEmpty())
                    .forEach(DB::insert);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("error sync data");
        }
    }
}
