package com.dynamsoft.farawayscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context ctx=this;
        Button liveScan = findViewById(R.id.liveScan);

        liveScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan(DCEActivity.class);
            }
        });
        Button liveScanCameraX = findViewById(R.id.liveScanCamaraX);
        liveScanCameraX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan(CameraActivity.class);
            }
        });
    }

    private void startScan(Class<?> target){
        if (hasCameraPermission()) {
            Intent intent = new Intent(this, target);
            startActivity(intent);
        } else {
            requestPermission();
            Toast.makeText(this, "Please grant camera permission" , Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    public void settingsButton_Clicked(View view){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void readImagesButton_Clicked(View view){
        Intent intent = new Intent(this, PictureActivity.class);
        startActivity(intent);
    }

    public void historyButton_Clicked(View view){
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }
}