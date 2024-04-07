package com.example.ttv;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.ttv.chatbox.chatbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SplashScreenActivity extends AppCompatActivity {
    Button equationSolver, storyTelling, illustration, contextImage;
    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        equationSolver = findViewById(R.id.equationSolver);
        storyTelling = findViewById(R.id.storyTelling);
        illustration = findViewById(R.id.illustration);
        contextImage = findViewById(R.id.btn_contextImage);

        equationSolver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSolve = new Intent(SplashScreenActivity.this, Home.class);
                startActivity(intentSolve);
            }
        });

        storyTelling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentVideo = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intentVideo);
            }
        });

        illustration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentVideo = new Intent(SplashScreenActivity.this, chatbox.class);
                startActivity(intentVideo);
            }
        });

        contextImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    Intent contextView = new Intent(SplashScreenActivity.this, Camera.class);
                    startActivity(contextView);

            }
        });
    }
}