package com.dynamsoft.farawayscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button enableCamera = findViewById(R.id.enableCamera);
        Context ctx=this;
        enableCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasCameraPermission()) {
                    Intent intent = new Intent(ctx, CameraActivity.class);
                    startActivity(intent);
                } else {
                    requestPermission();
                    Toast.makeText(ctx, "Please grant camera permission" , Toast.LENGTH_SHORT).show();
                }
            }
        });
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
}