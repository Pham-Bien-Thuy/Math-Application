package com.example.ttv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttv.ml.MobilenetV110224Quant;
import com.example.ttv.ml.SsdMobilenetV11Metadata1;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ObjectCaptureActivity extends AppCompatActivity {
    Button selectBtn, predictSingleBtn, predictMultiBtn, captureBtn;
    TextView result;
    ImageView imageView;
    Bitmap bitmap;

    String labelObject = null;
    String[] labelsMulti;
    String[] labelsSingle;
    static String objectDetector = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_capture);

        //permission
        getPermission();
        int maxSizeMulti = 92;
        int maxSizeSingle = 1002;
        labelsMulti = new String[maxSizeMulti];
        labelsSingle = new String[maxSizeSingle];
        int cntMulti = 0;
        int cntSingle = 0;
        try {
            BufferedReader bufferedReaderMulti = new BufferedReader(new InputStreamReader(getAssets().open("mobilenet_objectdetection_labels.txt")));
            BufferedReader bufferedReaderSingle = new BufferedReader(new InputStreamReader(getAssets().open("labels_mobilenet_quant_v1_224.txt")));
            String lineMulti = bufferedReaderMulti.readLine();
            String lineSingle = bufferedReaderSingle.readLine();
            while (lineMulti != null && cntMulti < maxSizeMulti) {
                labelsMulti[cntMulti] = lineMulti;
                cntMulti++;
                lineMulti = bufferedReaderMulti.readLine();
            }
            while (lineSingle != null && cntSingle < maxSizeSingle)
            {
                labelsSingle[cntSingle] = lineSingle;
                cntSingle++;
                lineSingle = bufferedReaderSingle.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        selectBtn = findViewById(R.id.selectBtn);
        predictSingleBtn = findViewById(R.id.predictSingleBtn);
        predictMultiBtn = findViewById(R.id.predictMultiBtn);
        captureBtn = findViewById(R.id.captureBtn);
        imageView = findViewById(R.id.imageViewClassify);
        result = findViewById(R.id.result);

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
            }
        });

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
            }
        });

        predictSingleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    MobilenetV110224Quant model = MobilenetV110224Quant.newInstance(ObjectCaptureActivity.this);

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
                    bitmap = Bitmap.createScaledBitmap(bitmap, 224,224, true);
                    inputFeature0.loadBuffer(TensorImage.fromBitmap(bitmap).getBuffer());

                    // Runs model inference and gets result.
                    MobilenetV110224Quant.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    result.setText("Result: "+ labelsSingle[getMax(outputFeature0.getFloatArray())] + " ");

                    // Releases model resources if no longer used.
                    model.close();

                } catch (IOException e) {
                    // TODO Handle the exception
                }

            }
        });

        predictMultiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                objectDetector = "";

                try {
                    SsdMobilenetV11Metadata1 model = SsdMobilenetV11Metadata1.newInstance(ObjectCaptureActivity.this);

                    // Creates inputs for reference.
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    // Runs model inference and gets result.
                    SsdMobilenetV11Metadata1.Outputs outputs = model.process(image);
                    TensorBuffer locations = outputs.getLocationsAsTensorBuffer();
                    TensorBuffer classes = outputs.getClassesAsTensorBuffer();
                    TensorBuffer scores = outputs.getScoresAsTensorBuffer();
                    TensorBuffer numberOfDetections = outputs.getNumberOfDetectionsAsTensorBuffer();

                    // Perform object detection for scores > 0.5
                    for (int i = 0; i < numberOfDetections.getFloatValue(0); i++) {
                        if (scores.getFloatValue(i) > 0.5) {

                            // Example: Print the class label and score
                            int classIndex = (int) classes.getFloatValue(i);
                            float score = scores.getFloatValue(i);
                            String className = labelsMulti[classIndex];
                            objectDetector += className + " ";
                            Log.d("ObjectDetection", "Class: " + className + ", Score: " + score);
                        }

                    }

                    result.setText("Result: " + objectDetector);
                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }
            }
        });
    }

    int getMax(float[] arr)
    {
        int max = 0;
        for (int i = 0; i < arr.length; i++)
        {
            if (arr[i] > arr[max])
            {
                  max = i;
            }
        }
        return max;
    }

    void getPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(ObjectCaptureActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 11)
        {
            if (grantResults.length > 0)
            {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                {
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 10)
        {
            if(data != null)
            {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else if (requestCode == 12)
        {
            bitmap = (Bitmap)data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}