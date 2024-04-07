package com.example.ttv;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.ttv.ocr_model.Assets;
import com.example.ttv.ocr_model.Config;
import com.example.ttv.ocr_model.MainViewModel;
import com.example.ttv.yolo_model.PrePostProcessor;
import com.example.ttv.yolo_model.ResultDetect;
import com.example.ttv.yolo_model.ResultView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    public static ArrayList<String> objectList = new ArrayList<>();
    ImageView clear, getImage, copy, voice, detectObject;
    EditText recgText;
    Uri imageOrigin;
    Uri imageCut;
    Bitmap bitmapDetectImage = null;
    Bitmap bitmapDetectText = null;
   // TextRecognizer textRecognizer;
    DatabaseReference lab411SRV;
    Button parsing;
//    static ArrayList<String> spanPersonName = new ArrayList<>();
    static ArrayList<ArrayList<String>> posDetector = new ArrayList<>();
    ArrayList<Span> spanArrayList = new ArrayList<>();
    JSONArray objectsList = new JSONArray();
    ArrayList<String> jsonData = new ArrayList<>();

    private ResultView mResultView;
    Module mModule = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;
    private MainViewModel viewModel;

    private static final int CAMERA_REQUEST_CODE=200;
    private static final int STORAGE_REQUEST_CODE=400;
    private static final int IMAGE_PICK_GALLERY_CODE=1000;
    private static final int IMAGE_PICK_CAMERA_CODE=1001;

    String[] cameraPermission;
    String[] storagePermission;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //camera permission
        cameraPermission=new String[]{Manifest.permission.CAMERA ,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //storage permission
        storagePermission=new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Copy sample image and language data to storage
        Assets.extractAssets(this);

        if (!viewModel.isInitialized()) {
            String dataPath = Assets.getTessDataPath(this);
            viewModel.initTesseract(dataPath, Config.TESS_LANG, Config.TESS_ENGINE);
        }

        clear = findViewById(R.id.clear);
        getImage = findViewById(R.id.getImage);
        voice = findViewById(R.id.voice);
        copy = findViewById(R.id.copy);
        recgText = findViewById(R.id.recgText);
        detectObject = findViewById(R.id.detect_object);
        parsing = findViewById(R.id.parsing);
//        objectImage = findViewById(R.id.iv_name_object);
//        mResultView = findViewById(R.id.result_view);
       // textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        lab411SRV = FirebaseDatabase.getInstance().getReference().child("TextToVideo");

        getImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                listedObject = "";
//                mResultView.setVisibility(View.INVISIBLE);
//                ImagePicker.with(MainActivity.this)
//                    .crop()	    			//Crop image(Optional), Check Customization for more option
//                    .compress(1024)			//Final image size will be less than 1 MB(Optional)
//                    .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
//                    .start();
                showImageImportDialog();
            }
        });

        voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });

        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = recgText.getText().toString();

                if (text.isEmpty()){
                    Toast.makeText(MainActivity.this, "No text to copy", Toast.LENGTH_SHORT).show();
                }
                else {
                    ClipboardManager clipboardManager = (ClipboardManager) getSystemService(MainActivity.this.CLIPBOARD_SERVICE);
                    ClipData clipData = ClipData.newPlainText("Data", recgText.getText().toString());
                    clipboardManager.setPrimaryClip(clipData);
                    Toast.makeText(MainActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
                    insertTextToVideoData();
                }
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = recgText.getText().toString();
                if (text.isEmpty()){
                    Toast.makeText(MainActivity.this, "No text to clear", Toast.LENGTH_SHORT).show();
                }
                else {
                    recgText.setText("");
                    Toast.makeText(MainActivity.this, "Clear", Toast.LENGTH_SHORT).show();
                }
            }
        });

        detectObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentObjectDetect = new Intent(MainActivity.this, TextObjectPairing.class);
                startActivity(intentObjectDetect);
            }
        });

        parsing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long startTime = System.currentTimeMillis();
                long consumeTime = 0;

                //Loading sentence detector model
                // Sentences Detect
                InputStream inputStream = null;
                SentenceModel model = null;
                String[] sentences = null;
                Span[] sentencesIndex = null;

                // POS Detect
                InputStream inputPos = null;
                POSModel posModel = null;
                ArrayList<ArrayList<String>> posIndex = new ArrayList<>();

                try {
                    inputStream = getAssets().open("en-sent.bin");
                    model = new SentenceModel(inputStream);

                    inputPos = getAssets().open("en-pos-maxent.bin");
                    posModel = new POSModel(inputPos);

                    spanArrayList.clear();
                    posDetector.clear();

                    if (model != null || posModel != null) {
                        SentenceDetectorME detector = new SentenceDetectorME(model);
                        POSTaggerME posTagger = new POSTaggerME(posModel);

                        sentences = detector.sentDetect(recgText.getText().toString());
                        sentencesIndex = detector.sentPosDetect(recgText.getText().toString());

                        if (sentences != null) {
                            spanArrayList = new ArrayList<>(Arrays.asList(sentencesIndex));
                        }

                        for (Span span : spanArrayList) {
                            int start = span.getStart();
                            int end = span.getEnd();

                            String extractedSentence = recgText.getText().toString().substring(start, end);

                            String joinSentence = String.join("", extractedSentence.split(","));

                            String[] tokens = joinSentence.split(" ");

                            ArrayList<String> spanPersonName = new ArrayList<>();

                            String tags[] = posTagger.tag(tokens);

                            ObjectItem objectItem = new ObjectItem();

                            for (int i=0; i<tags.length;i++) {
                                spanPersonName.add(tags[i] + ": " + tokens[i]);
                                if (tags[i].equals("NNP") || tags[i].equals("NN") || tags[i].equals("NNS") || tags[i].equals("NNPS") || tags[i].equals("PRP")) {
//                                    spanPersonName.add(tags[i] + ": " + tokens[i]);
                                    objectItem.setName(tokens[i]);
                                }
                                if (tags[i].equals("VBZ") || tags[i].equals("VB") || tags[i].equals("VBD") || tags[i].equals("VBG") || tags[i].equals("VBN") || tags[i].equals("VBP")) {
//                                    spanPersonName.add(tags[i] + ": " + tokens[i]);
                                    objectItem.setAction(tokens[i]);
                                }
                            }
                            objectItem.setTiming("first");
                            posDetector.add(spanPersonName);
                            objectsList = new JSONArray();
                            objectsList.put(objectToJson(objectItem));

                            JSONObject data = new JSONObject();
                            data.put("objects_list", objectsList);
                            jsonData.add(data.toString());
                        }
                    }
                    Log.d("TAG", jsonData.toString());

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                consumeTime = System.currentTimeMillis() - startTime;

                FragmentManager fm = getSupportFragmentManager();
                SentencesDialogFragment sentencesDialogFragment = new SentencesDialogFragment(spanArrayList, posDetector, jsonData, recgText.getText().toString(), consumeTime);

                if (fm.findFragmentByTag("fragment_answer_dialog") == null) {
                    sentencesDialogFragment.show(fm, "fragment_answer_dialog");
                }
            }
        });
        //Load model object detection with yolov5
//        try {
//            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "yolov5s.torchscript.ptl"));
//            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")));
//            String line;
//            List<String> classes = new ArrayList<>();
//            while ((line = br.readLine()) != null) {
//                classes.add(line);
//            }
//            PrePostProcessor.mClasses = new String[classes.size()];
//            classes.toArray(PrePostProcessor.mClasses);
//        } catch (IOException e) {
//            Log.e("Object Detection", "Error reading assets", e);
//            finish();
//        }
    }

    private static JSONObject objectToJson(ObjectItem myObject) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", myObject.getName());
        jsonObject.put("action", myObject.getAction());
        jsonObject.put("timing", myObject.getTiming());
        return jsonObject;
    }

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
        boolean result= ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1= ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return  result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    private void pickCamera() {
        //intent to take image from camera,it will also be saved to the storage to get high quality image
        ContentValues values= new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"NewPic");//title of image
        values.put(MediaStore.Images.Media.DESCRIPTION,"Image to text");//description
        imageOrigin=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        imageCut = imageOrigin;
        Intent cameraIntent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageOrigin);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
    }
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private void pickGallery() {
        //intent to pic image from gallery
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //got image from gallery now crop it
                assert data != null;
                CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON)//enable image guide line
                        .start(this);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                //got image from camera now crop it
                CropImage.activity(imageCut).setGuidelines(CropImageView.Guidelines.ON)//enable image guide line
                        .start(this);
            }
        }
        //get cropped images
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult activityResult = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                assert activityResult != null;
                imageCut = activityResult.getUri();//gete image uri
                try {
                    bitmapDetectText  = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageCut);
                    bitmapDetectImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageOrigin);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //Recognizing from image to text
                viewModel.recognizeImage(bitmapDetectText);
                viewModel.getResult().observe(MainActivity.this,result -> {
                    recgText.setText(result);
                });
//                recognizeText();
                    // Detect and recognizing name object in image
//                    detectObject();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                //if there is any error show it
                assert activityResult != null;
                Exception error = activityResult.getError();
                Toast.makeText(this, "" + error, Toast.LENGTH_SHORT).show();

            }
            else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SPEECH_INPUT){
                if (data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    recgText.setText(result.get(0));
                }
            }
            else {
                Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == Activity.RESULT_OK){
//            if (data != null){
//                imageUri = data.getData();
//                try {
//                    bitmapDetector  = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
//                    //Recognizing from image to text
//                    viewModel.recognizeImage(bitmapDetector);
//                    viewModel.getResult().observe(MainActivity.this,result -> {
//                        recgText.setText(result + "\nObject detect: " + listedObject);
//                    });
//                    // Detect and recognizing name object in image
//                    detectObject();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
//                //recognizeText();
//            }
//        }
//        else {
//            Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
//        }
//
//        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SPEECH_INPUT){
//            if (data != null){
//                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                recgText.setText(result.get(0));
//            }
//        }
//        else {
//            Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
//        }
//    }

//    private void recognizeText(){
//        if (imageCut != null){
//            try {
//                InputImage inputImage = InputImage.fromFilePath(MainActivity.this, imageCut);
//                Task<Text> result = textRecognizer.process(inputImage)
//                        .addOnSuccessListener(new OnSuccessListener<Text>() {
//                            @Override
//                            public void onSuccess(Text text) {
//                                String recognizeText = text.getText() + "\nObject detect: ";
//                                recgText.setText(recognizeText);
//                            }
//                        }).addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    private void insertTextToVideoData(){
        String data = recgText.getText().toString();

        TextToVideo text = new TextToVideo(data);
        lab411SRV.push().setValue(text);
        Toast.makeText(MainActivity.this, "Data Inserted", Toast.LENGTH_SHORT).show();

        Intent intentVideo = new Intent(MainActivity.this, LoadingActivity.class);
        startActivity(intentVideo);
    }

    private void speak(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi! speak something");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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

//    private void detectObject()
//    {
//        mImgScaleX = (float)bitmapDetectImage.getWidth() / PrePostProcessor.mInputWidth;
//        mImgScaleY = (float)bitmapDetectImage.getHeight() / PrePostProcessor.mInputHeight;
//
//        mIvScaleX = (bitmapDetectImage.getWidth() > bitmapDetectImage.getHeight() ? (float)objectImage.getWidth() / bitmapDetectImage.getWidth() : (float)objectImage.getHeight() / bitmapDetectImage.getHeight());
//        mIvScaleY  = (bitmapDetectImage.getHeight() > bitmapDetectImage.getWidth() ? (float)objectImage.getHeight() / bitmapDetectImage.getHeight() : (float)objectImage.getWidth() / bitmapDetectImage.getWidth());
//
//        mStartX = (objectImage.getWidth() - mIvScaleX * bitmapDetectImage.getWidth())/2;
//        mStartY = (objectImage.getHeight() -  mIvScaleY * bitmapDetectImage.getHeight())/2;
//
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmapDetectImage, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
//        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
//        final Tensor outputTensor = outputTuple[0].toTensor();
//        final float[] outputs = outputTensor.getDataAsFloatArray();
//        final ArrayList<ResultDetect> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);
//        mResultView.setResults(results);
//        mResultView.setVisibility(View.VISIBLE);
//    }
}