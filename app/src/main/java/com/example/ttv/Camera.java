package com.example.ttv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Camera extends AppCompatActivity {

    private ImageView imageView;
    private EditText editText;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap capturedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_give);

        imageView = findViewById(R.id.imageView);
        editText = findViewById(R.id.editText);

        Button btnCapture = findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(view -> dispatchTakePictureIntent());

        Button btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(view -> sendToServer());
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                capturedImage = (Bitmap) extras.get("data");
                imageView.setImageBitmap(capturedImage);
            }
        }
    }

    private void sendToServer() {
        if (capturedImage != null) {
            String imageData = convertImageToBase64(capturedImage);
            String textData = editText.getText().toString();

            // Tạo đối tượng JSONObject
            // Tạo đối tượng JSONObject
            JSONObject jsonObject = new JSONObject();
            try {
                // Đặt giá trị của các trường trong JSON object
                jsonObject.put("image_data", imageData);
                jsonObject.put("summary", textData);

                // Chuyển đối tượng JSONObject thành chuỗi JSON với dấu nháy kép
                String jsonData = jsonObject.toString().replaceAll("'", "\"");

                // Gửi yêu cầu
                if (isValidJson(jsonData)) {
                    OkHttpClient client = new OkHttpClient();
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonData);

                    Request request = new Request.Builder()
                            .url("http://10.136.205.174:5000/todos/1")
                            .post(requestBody)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.d("Upload", "Failed to upload", e);
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.code() == 303) {
                                String redirectUrl = response.header("Location");
                                if (redirectUrl != null) {
                                    handleRedirect(redirectUrl);
                                }
                            } else if (response.isSuccessful()) {
                                String responseData = response.body().string();
                                Log.d("Upload", "Upload successful: " + responseData);
                                // Handle the success message here
                            } else {
                                Log.d("Upload", "Upload failed. Response code: " + response.code());
                            }
                        }
                    });
                } else {
                    Log.d("JSON", "Invalid JSON format");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private boolean isValidJson(String jsonString) {
        try {
            new JSONObject(jsonString);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private void handleRedirect(String redirectUrl) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(redirectUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Redirect", "Failed to follow redirect", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d("Redirect", "Redirect successful");
                    // Handle the redirected response if needed
                } else {
                    Log.d("Redirect", "Redirect failed");
                }
            }
        });
    }

    private String convertImageToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
