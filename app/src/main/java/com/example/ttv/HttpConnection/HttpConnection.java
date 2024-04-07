package com.example.ttv.HttpConnection;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpConnection {
    private OkHttpClient client;
    private static HttpConnection instance = new HttpConnection();

    public static HttpConnection getInstance() {
        return instance;
    }

    private HttpConnection(){ this.client = new OkHttpClient(); }

    public void requestWebServer() {
        Request request = new Request.Builder()
                .url(Constant.url_get)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body().string();
            }
        });
    }

    public void responseWebServer()
    {
        RequestBody formBody = new FormBody.Builder().add("","").add("", "").build();
        Request request = new Request.Builder()
                .url(Constant.url_post)
                .post(formBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String res = response.body().string();
            }
        });
    }
}
