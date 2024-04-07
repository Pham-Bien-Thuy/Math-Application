package com.example.ttv;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Objects;

public class MathSolve extends AppCompatActivity {
    SurfaceView cameraView;
    TextView textView;
    CameraSource cameraSource;
    ImageButton cameraMode, keyboardMode, drawMode;
    EditText expressionEditText;
    RelativeLayout layout;
    TextView result;
    Button getSolution;
    private int mode = 0;
    public static final String TO_SOLUTIONS_LABEL = "Solutions";
    private static final String TO_SOLUTIONS = "Let's go to the solutions!!!!!!";
    public static final String INPUT_LABEL = "Input";

    private static final int requestPermissionID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_solve);

        cameraView = findViewById(R.id.surfaceView);
        // textView = findViewById(R.id.textView);
        cameraMode = findViewById(R.id.cameraMode);
        keyboardMode = findViewById(R.id.keyboardMode);
        drawMode = findViewById(R.id.drawMode);
        expressionEditText = findViewById(R.id.expressionEditText);
        layout = findViewById(R.id.relativeLayout);
        result = findViewById(R.id.result);
        getSolution = findViewById(R.id.getSolution);

        startCameraSource();

        getSolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toSolutionsActivity = new Intent(MathSolve.this, SolutionActivity.class);
                toSolutionsActivity.putExtra(TO_SOLUTIONS_LABEL, TO_SOLUTIONS);
                // String finalResult = Objects.requireNonNull(tl_comment.getEditText()).getText().toString();
                toSolutionsActivity.putExtra(INPUT_LABEL, result.getText());
                startActivity(toSolutionsActivity);
            }
        });

        cameraMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraView.getVisibility() != View.VISIBLE) {
                    cameraView.setVisibility(View.VISIBLE);
                    layout.setVisibility(View.GONE);
                    result.setVisibility(View.VISIBLE);

                    try {
                        if (ActivityCompat.checkSelfPermission(MathSolve.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // textView.setText("");
            }
        });

        keyboardMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraView.getVisibility() == View.VISIBLE) {
                    cameraSource.stop();
                    cameraView.setVisibility(View.GONE);
                    layout.setVisibility(View.VISIBLE);
                    result.setVisibility(View.VISIBLE);
                }
                // textView.setText("");
            }
        });

        drawMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraSource.stop();
                cameraView.setVisibility(View.GONE);
                layout.setVisibility(View.GONE);
                result.setVisibility(View.GONE);

                Intent intentDraw = new Intent(MathSolve.this, Draw.class);
                startActivity(intentDraw);
            }
        });

        expressionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    // textView.setText(new MathEvaluation(s.toString().replace("\n", "")).parse());
                    result.setText(expressionEditText.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // startCameraSource();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestPermissionID && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraSource.start(cameraView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(MathSolve.this, "Permission not Granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCameraSource() {
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (textRecognizer.isOperational()) {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(4.0f)
                    .build();

            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CALL_COMPANION_APP) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MathSolve.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        result.post(() -> {
                            try {
                                // textView.setText(new MathEvaluation(items.valueAt(0).getValue()).parse());
                                result.setText(new MathEvaluation(items.valueAt(0).getValue()).showResult());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            });
        }
    }
}