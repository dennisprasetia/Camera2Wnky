package com.wonokoyo.camera2wnky;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button btnGoToCamera;
    private Button btnGoToRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] stringPermissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET};
        ActivityCompat.requestPermissions(MainActivity.this, stringPermissions, 50);

        btnGoToCamera = findViewById(R.id.btnGoToCamera);
        btnGoToCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            }
        });

        btnGoToRecord = findViewById(R.id.btnGoToRecord);
        btnGoToRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, VideoRecordActivity.class));
            }
        });
    }
}
