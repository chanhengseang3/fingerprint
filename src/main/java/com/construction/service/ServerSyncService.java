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
import java.util.logging.Logger;

public class ServerSyncService {

    private static final DbConnector DB = new DbConnector();
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(ServerSyncService.class.getSimpleName());

    private static final String REGISTER_URL = "register.url";
    private static final String GET_ALL = "get.all.url";
    private static final String GET_PENDING = "get.pending.url";
    private static final String SEND_VERIFY_USER = "send.verified.url";

    public static SubConstructor addSubConstructorToRemoteServer(SubConstructor subConstructor) {
        String url = PropertyUtil.getProperty(REGISTER_URL);
        String body = GSON.toJson(subConstructor);
        Response response = HttpUtil.post(url, body);
        if (200 != response.code()) {
            return null;
        }
        try {
            String result = Objects.requireNonNull(response.body()).string();
            return GSON.fromJson(result, SubConstructor.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SubConstructor sendVerifiedUser(int userId) {
        LOGGER.info("send a verification result to server");
        String url = String.format(PropertyUtil.getProperty(SEND_VERIFY_USER), userId);
        Response response = HttpUtil.post(url);
        if (200 != response.code()) {
            return null;
        }
        try {
            String result = Objects.requireNonNull(response.body()).string();
            return GSON.fromJson(result, SubConstructor.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<SubConstructor> getPendingSubConstructor() throws IOException {
        String url = PropertyUtil.getProperty(GET_PENDING);
        Response response = HttpUtil.get(url);
        if (200 != response.code()) {
            throw new RuntimeException("Fail to get list");
        }
        String result = Objects.requireNonNull(response.body()).string();
        Type typeToken = new TypeToken<List<SubConstructor>>() {
        }.getType();
        return GSON.fromJson(result, typeToken);
    }

    public static boolean syncFromServer() {
        try {
            String url = PropertyUtil.getProperty(GET_ALL);
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
