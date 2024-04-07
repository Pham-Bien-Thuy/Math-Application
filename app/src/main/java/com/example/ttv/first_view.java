package com.example.ttv;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.ttv.chatbox.chatbox;

public class first_view extends AppCompatActivity {

    private Button get_started;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_view);

        get_started = findViewById(R.id.btnGet_Started);
        get_started.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityHome();
            }
        });
    }
    public void openActivityHome()
    {
        Intent intent = new Intent(this, chatbox.class);
        startActivity(intent);
    }
}