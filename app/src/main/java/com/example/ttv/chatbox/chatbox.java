package com.example.ttv.chatbox;

import static com.example.ttv.chatbox.ResultViews.listedObject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ttv.DisplayActivity;
import com.example.ttv.Home;
import com.example.ttv.ObjectListActivity;
import com.example.ttv.R;
import com.example.ttv.SplashScreenActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class chatbox extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton, getImage,getvideo,getVoice;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    // Detect text
    TextRecognizer textRecognizer;
    //Camera
    String[] cameraPermission;
    String[] storagePermission;
    Uri imageOrigin;
    Uri imageCut;
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 400;
    private static final int IMAGE_PICK_GALLERY_CODE = 1000;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    private Button editButton;
    private Button cancelButton;

    //private List<ResultDetect> results;
    //Detect object
    Module mModule = null;
    Bitmap bitmapDetector = null;
    private ResultViews mResultView;
    ImageView objectImage;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;
    String imageObjectBitmap;


//    public static ArrayList<String> objectList = new ArrayList<>();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbox);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);
        getImage = findViewById(R.id.getImage);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        mResultView = findViewById(R.id.result_view);
        objectImage = findViewById(R.id.iv_name_object);
        //change activate video view
        getvideo = (ImageButton) findViewById(R.id.getvideo);
        getVoice = findViewById(R.id.getVoice);




        getvideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(chatbox.this, DisplayActivity.class);
                startActivity(intent);
            }
        });
        getVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSolve = new Intent(chatbox.this, Home.class);
                startActivity(intentSolve);
            }
        });

        //camera permission
        cameraPermission = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v) -> {
            String question = messageEditText.getText().toString().trim();
            addToChat(question, Message.SENT_BY_ME, null);
            messageEditText.setText("");
            callAPI(question);
            welcomeTextView.setVisibility(View.GONE);

        });
        getImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listedObject = "";
                mResultView.setVisibility(View.INVISIBLE);
                showImageImportDialog();
            }
        });

        //Load model object detection with yolov5
        try {
            mModule = LiteModuleLoader.load(chatbox.assetFilePath(getApplicationContext(), "yolov5s.torchscript.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcess.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcess.mClasses);
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // Got image from gallery, now crop it
                assert data != null;
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // Got image from camera, now crop it
                CropImage.activity(imageCut)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
            }
        }

        // Get cropped images
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult activityResult = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert activityResult != null;
                imageCut = activityResult.getUri();  // Get image URI
                try {
                    bitmapDetector = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageCut);
                    imageObjectBitmap = convertImageToBase64(bitmapDetector);
                    recognizeText();
                    detectObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                // If there is any error, show it
                assert activityResult != null;
                Exception error = activityResult.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    void addToChat(String message, String sentBy, Bitmap image) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (message != null && !message.isEmpty()) {
                    // Display the text
                    messageList.add(new Message(message, sentBy, null));
                }
                if (image != null) {
                    // Display the image
                    messageList.add(new Message(null, sentBy, image));
                }



                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }


    void addResponse(String response) {
        messageList.remove(messageList.size() - 1);
        addToChat(response, Message.SENT_BY_BOT, null);
        sendToServer(response,null,null);
    }

    private void sendToServer(String response, String objName, Bitmap objImage) {
        if (objName != null || (response != null && !response.isEmpty())) {
            // Tạo đối tượng JSONObject chứa thông tin đoạn chat
            JSONObject jsonBody = new JSONObject();
            try {
                // Tạo đối tượng JSONArray chứa tất cả các đoạn chat và đối tượng hình ảnh
                JSONArray chatArray = new JSONArray();
                JSONArray objArray = new JSONArray();
                // Thêm response của GPT vào đối tượng chat nếu có
                if (response != null && !response.isEmpty()) {
                    JSONObject chat = new JSONObject();
                    chat.put("text", response);
//                    chat.put("subject","Two chickens plus three chickens");
//                    chat.put("relation", "equals");
//                    chat.put("object", "five chickens");

                    chatArray.put(chat);

                }

                // Thêm các đối tượng hình ảnh và tên obj vào objArray
                if (objName != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("obj_name", objName);

                    // Convert Bitmap to base64
                    String encodedImage = convertImageToBase64(objImage);
                    obj.put("encoder_obj_img", encodedImage);

                    objArray.put(obj);
                }

                // Thêm chatArray vào đối tượng jsonBody
                jsonBody.put("chat", chatArray);
                jsonBody.put("obj", objArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Gửi yêu cầu
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

            // Thay đổi URL của server endpoint
            Request request = new Request.Builder()
                    .url("http://10.136.117.90:5000/result") // Thay đổi thành địa chỉ endpoint thực tế của server
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
                        // Xử lý thông báo thành công ở đây
                    } else {
                        Log.d("Upload", "Upload failed. Response code: " + response.code());
                    }
                }
            });
        } else {
            Log.d("Upload", "No messages to send.");
        }
    }



    void callAPI(String question) {
        // okhttp
        messageList.add(new Message("Typing... ", Message.SENT_BY_BOT, bitmapDetector));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo-instruct");

            jsonBody.put("prompt", question);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    addResponse("Failed to load response due to " + response.body().toString());
                }
            }
        });
    }

    // Hàm xử lý redirect, bạn có thể thêm logic xử lý tại đây
    private void handleRedirect(String redirectUrl) {
        // TODO: Xử lý redirect
    }


    //Handle Images from camera and gallery
    private void showImageImportDialog() {
        //item to displayin dialog
        String[] items = {"Camera", "Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    //camera option clicked
                    if (!checkCameraPermission()) {
                        //camera permission not allowed,request it
                        requestCameraPermission();
                    } else {
                        //permission allowed,take picture
                        pickCamera();
                    }
                }
                if (which == 1) {
                    //gallery option clicked
                    if (!checkStoragePermission()) {
                        //Storage permission not allowed,request it
                        requestStoragePermission();
                    } else {
                        //permission allowed,take picture
                        pickGallery();
                    }
                }
            }
        });
        dialog.create().show();//show dialog
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private void pickCamera() {
        //intent to take image from camera,it will also be saved to the storage to get high quality image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPic");//title of image
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image to text");//description
        imageOrigin = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        imageCut = imageOrigin;
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageOrigin);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private void pickGallery() {
        //intent to pic image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        pickCamera();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case STORAGE_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        pickGallery();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    private String convertImageToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    private void detectObject() {
        if (imageCut != null) {
            try {
                // Convert imageCut to Bitmap
                Bitmap bitmapDetector = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageCut);

                // Continue with the existing detection logic
                mImgScaleX = (float) bitmapDetector.getWidth() / PrePostProcess.mInputWidth;
                mImgScaleY = (float) bitmapDetector.getHeight() / PrePostProcess.mInputHeight;

                mIvScaleX = (bitmapDetector.getWidth() > bitmapDetector.getHeight() ? (float) objectImage.getWidth() / bitmapDetector.getWidth() : (float) objectImage.getHeight() / bitmapDetector.getHeight());
                mIvScaleY = (bitmapDetector.getHeight() > bitmapDetector.getWidth() ? (float) objectImage.getHeight() / bitmapDetector.getHeight() : (float) objectImage.getWidth() / bitmapDetector.getWidth());

                mStartX = (objectImage.getWidth() - mIvScaleX * bitmapDetector.getWidth()) / 2;
                mStartY = (objectImage.getHeight() - mIvScaleY * bitmapDetector.getHeight()) / 2;

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapDetector, PrePostProcess.mInputWidth, PrePostProcess.mInputHeight, true);
                final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcess.NO_MEAN_RGB, PrePostProcess.NO_STD_RGB);
                IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
                final Tensor outputTensor = outputTuple[0].toTensor();
                final float[] outputs = outputTensor.getDataAsFloatArray();
                final ArrayList<ResultDetect> results = PrePostProcess.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);


                Bitmap resize=Bitmap.createScaledBitmap(bitmapDetector,objectImage.getWidth(),objectImage.getHeight(),true);

                // Tạo danh sách để lưu trữ các đối tượng
                List<ObjectData> objectList = new ArrayList<>();

                for(ResultDetect result : results ){
                    Bitmap Crop= Bitmap.createBitmap(resize,(int)result.getLeft(), (int)result.getTop(), (int)result.getRight()-(int)result.getLeft(), (int)result.getBottom() - (int)result.getTop());
                    objectList.add(new ObjectData(result.objectName, Crop));
//                    addToChat(result.objectName ,Message.SENT_BY_ME, Crop);
//                    sendToServer(null, result.objectName, Crop);

                }
                // Gửi danh sách các đối tượng lên server
                goToObjectListActivity(objectList);
                sendObjectListToServer(objectList);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void goToObjectListActivity(List<ObjectData> objectList) {
        Intent intent = new Intent(this, ObjectListActivity.class);
        intent.putParcelableArrayListExtra("objectList", (ArrayList<? extends Parcelable>) new ArrayList<>(objectList));
        startActivity(intent);
    }
    private void sendObjectListToServer(List<ObjectData> objectList) {
        // Kiểm tra xem danh sách có phần tử không
        if (objectList != null && !objectList.isEmpty()) {
            // Tạo đối tượng JSONObject chứa thông tin đối tượng
            JSONObject jsonBody = new JSONObject();
            try {
                // Tạo đối tượng JSONArray chứa tất cả các đối tượng hình ảnh và tên obj
                JSONArray objArray = new JSONArray();

                // Thêm từng đối tượng vào objArray
                for (ObjectData objectData : objectList) {
                    JSONObject obj = new JSONObject();
                    obj.put("obj_name", objectData.getObjName());

                    // Convert Bitmap to base64
                    String encodedImage = convertImageToBase64(objectData.getObjImage());
                    obj.put("encoder_obj_img", encodedImage);

                    objArray.put(obj);
                }

                // Thêm objArray vào đối tượng jsonBody
                jsonBody.put("obj", objArray);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Gửi yêu cầu
            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());

            // Thay đổi URL của server endpoint
            Request request = new Request.Builder()
                    .url("http://10.136.211.13:5000/result") // Thay đổi thành địa chỉ endpoint thực tế của server
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
                        // Xử lý thông báo thành công ở đây
                    } else {
                        Log.d("Upload", "Upload failed. Response code: " + response.code());
                    }
                }
            });
        } else {
            Log.d("Upload", "No objects to send.");
        }
    }

    private void recognizeText() {
        if (imageCut != null) {
            try {
                InputImage inputImage = InputImage.fromFilePath(chatbox.this, imageCut);
                Task<Text> result = textRecognizer.process(inputImage)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text text) {
                                addToChat(text.getText(), Message.SENT_BY_ME, null);
                                callAPI(text.getText());
                                welcomeTextView.setVisibility(View.GONE);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(chatbox.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}