package com.hfutonline.hc.utils;

import com.alibaba.fastjson.JSON;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.Map;
import java.util.Objects;

/**
 * @author chenliangliang
 * @date 2019/3/18
 */
public class OkHttpUtil {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.6 Safari/537.36";

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");


    private OkHttpUtil() {
    }

    public static Request newGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .header("Connection", "keep-alive")
                .header("User-Agent", USER_AGENT)
                .get()
                .build();
    }

    public static Request newPostRequest(String url, MediaType type, String content) {
        return new Request.Builder()
                .url(url)
                .header("Connection", "keep-alive")
                .header("User-Agent", USER_AGENT)
                .post(RequestBody.create(type, content))
                .build();
    }


    public static Request newPostJsonRequest(String url, Object obj) {
        return new Request.Builder()
                .url(url)
                .header("Connection", "keep-alive")
                .header("User-Agent", USER_AGENT)
                .post(RequestBody.create(MEDIA_TYPE_JSON, JSON.toJSONBytes(obj)))
                .build();
    }

    public static Request newPostFormRequest(String url, Map<String, String> formData) {
        Objects.requireNonNull(formData, "formData must not be null!");

        FormBody.Builder formBodyBuilder = new FormBody.Builder();

        formData.forEach(formBodyBuilder::add);

        return new Request.Builder()
                .url(url)
                .header("Connection", "keep-alive")
                .header("User-Agent", USER_AGENT)
                .post(formBodyBuilder.build())
                .build();
    }
}
