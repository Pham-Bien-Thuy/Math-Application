package com.example.ttv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RemoveBackground extends AppCompatActivity {
    ImageView imgOrigin, imgRemoveBackground;
    Button btn_removeBackground;
    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remove_background);
        imgOrigin = findViewById(R.id.imgOrigin);
        imgRemoveBackground = findViewById(R.id.imgRemoveBackground);


        Uri receivedUri = getIntent().getData();

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), receivedUri);
            imgOrigin.setImageBitmap(bitmap);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        btn_removeBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadServerImage(bitmap);
            }
        });
    }
    private void uploadServerImage(Bitmap bitmap)
    {
        // Chuyển đổi bitmap thành mảng byte
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageBytes = stream.toByteArray();
        //OkHttpClient client = new OkHttpClient();
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.MINUTES)
                .build();
        // Tạo requestBody từ mảng byte
        RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), imageBytes);

        // Tạo request
        Request request = new Request.Builder()
                .url("http://192.168.1.3:5000/")
                .post(requestBody)
                .build();

        // Thực thi request bất đồng bộ
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Xử lý khi gửi thất bại
                Log.d("Failer", "Fail");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update UI elements here
                        if (response.isSuccessful()) {
                            // Get the input stream from the response body
                            InputStream inputStream = response.body().byteStream();
                            // Decode the input stream into a Bitmap
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            // Set the Bitmap in the ImageView
                            imgRemoveBackground.setImageBitmap(bitmap);
                        }
                    }
                });

            }
        });

    }
}