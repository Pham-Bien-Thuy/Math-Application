package com.example.ttv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageContextResult extends AppCompatActivity {
    Bitmap bitmap;
    String contextView;
    Button btnContext;
    ImageView viewContext;
    TextView textContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_context_result);

        viewContext = findViewById(R.id.img_contextView);
        btnContext = findViewById(R.id.btn_contextImage);
        textContext = findViewById(R.id.tv_context);

//        videoView = findViewById(R.id.videoDisplay);
//        lab411SRV = FirebaseDatabase.getInstance().getReference().child("Display/link");
//        inputText = findViewById(R.id.inputText);
        //videoView.setMediaController(mediaController);
        //mediaController.setAnchorView(videoView);

//        ArrayList<String> dataListDetector = intent.getStringArrayListExtra("dataListDetector");
        //link = intent.getStringExtra("videoPath");
        Uri receivedUri = getIntent().getData();

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), receivedUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Convert the byte array back to a bitmap
        //bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        viewContext.setImageBitmap(bitmap);
        //inputText.setText("a young girl in a bustling schoolyard, her books clutched tightly to her chest, a young girl by Lisa, her books clutched tightly to her chest \", A boy walked to her ,he approached and handed her a bouquet of flowers, he and her spent the afternoon walking hand in hand through the schoolyard, they kissed,they wave and say goodbye to each other\".");
        //videoView.setVideoPath(link);

        //videoView.start();

//        lab411SRV.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                // Retrieve the link from the data snapshot
////                link = dataSnapshot.getValue(String.class);
//                link = "http://192.168.0.125:5000/get_video?name=" + dataListDetector.get(0);
//                Log.d("TAG", link);
//
//                // Pass the link to the video player
//                uri = Uri.parse(link);
//                videoView.setVideoURI(uri);
//                videoView.start();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                // Handle any errors
//                // ...
//            }
//        });

        // uri = Uri.parse("https://firebasestorage.googleapis.com/v0/b/lab411-srv-3dbd2.appspot.com/o/SampleVideo_1280x720_1mb.mp4?alt=media&token=8c770d1d-a7da-4020-9e96-9f607e1e129c");
        //uri = Uri.parse(link);
        //videoView.setVideoURI(uri);
        //videoView.start();

        btnContext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                .url("http://192.168.0.106:5000/image_context")
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
                            try {
                                String res = response.body().string();
                                textContext.setText("Context: " + res);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            // Update UI elements using 'res' here
                        }
                    }
                });
//                // Xử lý khi gửi thành công
//                if (response.isSuccessful()) {
//
//                    String res = response.body().string();
//                    textContext.setText("Context: " + "japp");
////                    Intent intent = new Intent(ImageContext.this, ImageContextResult.class);
////                    //intent.putExtra("bitmap", bitmap);
////                    startActivity(intent);
//                    Log.d("Successs: ", res);
////                    BottomSheetEquation addPhotoBottomDialogFragment = BottomSheetEquation.newInstance();
////                    BottomSheetEquation addPhotoBottomDialog = new BottomSheetEquation(tl);
////                    addPhotoBottomDialog.show(getSupportFragmentManager(), "add_photo_dialog_fragment");
//                }
            }
        });

    }
}