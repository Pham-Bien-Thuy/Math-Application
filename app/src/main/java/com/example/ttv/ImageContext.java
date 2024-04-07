package com.example.ttv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageContext extends AppCompatActivity {

    private CameraView camera;
    private ImageButton btn_capture, btn_flash;
    public static final int TAKE_PHOTO = 150;
    public static final int CHOOSE_PHOTO = 300;
    public static final String FILE_NAME = "temp.jpg";

    String currentPhotoPath;

    CropImageView cropImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_context);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        camera = findViewById(R.id.camera);

        camera.setLifecycleOwner(this);
        camera.addCameraListener(new Listener());

        btn_capture = findViewById(R.id.btn_capture);
        btn_flash = findViewById(R.id.btn_flash);
        
        btn_flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (camera.getFlash()== Flash.OFF){
                    camera.setFlash(Flash.TORCH);
                    btn_flash.setImageDrawable(getDrawable(R.drawable.ic_flash_on_black_24dp));
                } else {
                    btn_flash.setImageDrawable(getDrawable(R.drawable.ic_flash_off_black_24dp));
                    camera.setFlash(Flash.OFF);
                }
            }
        });

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                capturePicture();
            }
        });

        checkInternet();
    }

    private void showNetDialog(){
        new AlertDialog.Builder(ImageContext.this)
                .setTitle("Warning")
                .setMessage("Connect to internet via WIFI or mobile data " +
                        "to be able to use the app.")
                .setCancelable(false)
                .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ImageContext.this.recreate();
                    }
                }).show();
    }

    private void checkInternet(){
        ConnectivityManager mgr = (ConnectivityManager) ImageContext.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = Objects.requireNonNull(mgr).getActiveNetworkInfo();

        if (netInfo != null) {
            if (netInfo.isConnected()) {
                // Internet Available
            }else {
                //No internet
                showNetDialog();
            }
        } else {
            //No internet
            showNetDialog();
        }
    }

    private void runTextRecognition1(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        //mTextButton.setEnabled(false);
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text texts) {
                                Toast.makeText(ImageContext.this,"Result: "+texts.getText(),Toast.LENGTH_LONG).show();
                                //mTextButton.setEnabled(true);
                                //processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                //mTextButton.setEnabled(true);
                                e.printStackTrace();
                            }
                        });
    }
    private void processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        Toast.makeText(ImageContext.this,"Result: "+texts.getText(),Toast.LENGTH_LONG).show();

        if (blocks.size() == 0) {
            ///showToast("No text found");
            return;
        }
        //mGraphicOverlay.clear();
        for (int i = 0; i < blocks.size(); i++) {
            List<Text.Line> lines = blocks.get(i).getLines();
            for (int j = 0; j < lines.size(); j++) {
                List<Text.Element> elements = lines.get(j).getElements();
                for (int k = 0; k < elements.size(); k++) {

                    //Graphic textGraphic = new TextGraphic(mGraphicOverlay, elements.get(k));
                    //mGraphicOverlay.add(textGraphic);

                }
            }
        }
    }
    private class Listener extends CameraListener {

        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {

        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            //message("Got CameraException #" + exception.getReason(), true);
        }

        public Uri getImageUri(Context inContext, Bitmap inImage) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
            return Uri.parse(path);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
            if (camera.isTakingVideo()) {

                //message("Captured while taking video. Size=" + result.getSize(), false);
                //return;
            }
            result.toBitmap(new BitmapCallback() {
                @SuppressLint("WrongThread")
                @Override
                public void onBitmapReady(@Nullable Bitmap bitmap) {
                    // Context Image
                    Uri uri = getImageUri(ImageContext.this, bitmap);
                    Intent intent = new Intent(ImageContext.this, ImageContextResult.class);
                    intent.setData(uri);
                    startActivity(intent);

                    // Remove Background
//                    Uri uriRemove = getImageUri(ImageContext.this, bitmap);
//                    Intent intentRemove = new Intent(ImageContext.this, RemoveBackground.class);
//                    intentRemove.setData(uriRemove);
//                    startActivity(intentRemove);



                    //runTextRecognition(bitmap);
//                    onDeviceRecognition(bitmap);
//                    cropImageView.setImageBitmap(bitmap);
//                    //cropImageView.getCroppedImageAsync();
//                    cropImageView.setVisibility(View.INVISIBLE);

                    //Bitmap cropped = cropImageView.getCroppedImage();
                    //uploadServerImage(cropped);
                }
            });
        }
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
                // Xử lý khi gửi thành công
                if (response.isSuccessful()) {

                    String tl = response.body().string();
//                    Intent intent = new Intent(ImageContext.this, ImageContextResult.class);
//                    //intent.putExtra("bitmap", bitmap);
//                    startActivity(intent);

                    Log.d("Successs: ", tl);
//                    BottomSheetEquation addPhotoBottomDialogFragment = BottomSheetEquation.newInstance();
//                    BottomSheetEquation addPhotoBottomDialog = new BottomSheetEquation(tl);
//                    addPhotoBottomDialog.show(getSupportFragmentManager(), "add_photo_dialog_fragment");
                }
            }
        });

    }

    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getCloudTextRecognizer();
        detector.processImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText texts) {
                processExtractedText(texts);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure
                    (@NonNull Exception exception) {
                Toast.makeText(ImageContext.this,
                        "Exception", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processExtractedText(FirebaseVisionText firebaseVisionText) {
        //myTextView.setText(null);
        if (firebaseVisionText.getTextBlocks().size() == 0) {
            //myTextView.setText(R.string.no_text);

            return;
        }
        for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
            //myTextView.append(block.getText());

            TextView tv_result = findViewById(R.id.tv_result);
            tv_result.setText(block.getText());
//            BottomSheetEquation addPhotoBottomDialogFragment = BottomSheetEquation.newInstance();
//            BottomSheetEquation addPhotoBottomDialog = new BottomSheetEquation(block.getText());
//            addPhotoBottomDialog.show(getSupportFragmentManager(), "add_photo_dialog_fragment");
        }

    }

    private void onDeviceRecognition(Bitmap bitmap){

        cropImageView.setImageBitmap(bitmap);
        //cropImageView.getCroppedImageAsync();
        cropImageView.setVisibility(View.INVISIBLE);

        Bitmap cropped = cropImageView.getCroppedImage();

        recognition(cropped);

    }

    private void recognition(Bitmap cropped){
        runTextRecognition1(cropped);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(Objects.requireNonNull(cropped));
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        // ...
                        String resultText = result.getText();
                        TextView tv_result = findViewById(R.id.tv_result);
                        tv_result.setText(resultText);

//                        BottomSheetEquation addPhotoBottomDialogFragment = BottomSheetEquation.newInstance();
//                        BottomSheetEquation addPhotoBottomDialog = new BottomSheetEquation(result.getText());
//                        addPhotoBottomDialog.show(getSupportFragmentManager(), "add_photo_dialog_fragment");

                        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
                            String blockText = block.getText();
                            Float blockConfidence = block.getConfidence();
                            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
                            Point[] blockCornerPoints = block.getCornerPoints();
                            Rect blockFrame = block.getBoundingBox();

                            for (FirebaseVisionText.Line line: block.getLines()) {
                                String lineText = line.getText();
                                Float lineConfidence = line.getConfidence();
                                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                                Point[] lineCornerPoints = line.getCornerPoints();
                                Rect lineFrame = line.getBoundingBox();
                                for (FirebaseVisionText.Element element: line.getElements()) {
                                    String elementText = element.getText();
                                    Float elementConfidence = element.getConfidence();
                                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                                    Point[] elementCornerPoints = element.getCornerPoints();
                                    Rect elementFrame = element.getBoundingBox();

                                }
                            }
                        }

                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });
    }
    private void capturePicture() {
        if (camera.getMode() == Mode.VIDEO) {
            //message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) return;
        long mCaptureTime = System.currentTimeMillis();
        // message("Capturing picture...", false);
        camera.takePicture();
        Log.d("Capture:" , "capture");
    }

    private void saveToInternalStorage(Bitmap bitmapImage){
        File storageDir = this.getExternalFilesDir("images");
        // Create imageDir
        File mypath = new File(storageDir,"captured_image.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void startCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, TAKE_PHOTO);
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.raajesharunachalam.mathsolver.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, CHOOSE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_PHOTO) {
            Uri imageUri = data.getData();


            // start cropping activity for pre-acquired image saved on the device
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .start(this);

        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));
                    recognition(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "";

        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();
        if (labels != null && !labels.isEmpty()) {
            message += String.format(Locale.US, "%s", labels.get(0).getDescription());
        } else {
            message += "nothing";
        }

        return message;
    }

}