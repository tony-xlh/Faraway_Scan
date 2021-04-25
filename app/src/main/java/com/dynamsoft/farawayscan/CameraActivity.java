package com.dynamsoft.farawayscan;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.BarcodeReaderException;
import com.dynamsoft.dbr.DBRLTSLicenseVerificationListener;
import com.dynamsoft.dbr.DMLTSConnectionParameters;
import com.dynamsoft.dbr.EnumImagePixelFormat;
import com.dynamsoft.dbr.TextResult;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView resultView;
    private ExecutorService exec;
    private Camera camera;
    private BarcodeReader dbr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        resultView = findViewById(R.id.resultView);
        exec = Executors.newSingleThreadExecutor();
        try {
            dbr = new BarcodeReader();
        } catch (BarcodeReaderException e) {
            e.printStackTrace();
        }
        DMLTSConnectionParameters parameters = new DMLTSConnectionParameters();
        parameters.organizationID = "200001";
        dbr.initLicenseFromLTS(parameters, new DBRLTSLicenseVerificationListener() {
            @Override
            public void LTSLicenseVerificationCallback(boolean isSuccess, Exception error) {
                if (!isSuccess) {
                    error.printStackTrace();
                }
            }
        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreviewAndImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        SeekBar zoomRatioSeekBar = findViewById(R.id.zoomRatioSeekBar);
        zoomRatioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    camera.getCameraControl().setLinearZoom((float) progress / 100);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private void bindPreviewAndImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Size resolution = new Size(720, 1280);
        Display d = getDisplay();
        if (d.getRotation() != Surface.ROTATION_0) {
            resolution = new Size(1280, 720);
        }

        Preview.Builder previewBuilder = new Preview.Builder();
        previewBuilder.setTargetResolution(resolution);
        Preview preview = previewBuilder.build();

        ImageAnalysis.Builder imageAnalysisBuilder = new ImageAnalysis.Builder();

        //Camera2Interop.Extender ext = new Camera2Interop.Extender<>(imageAnalysisBuilder);
        //ext.setCaptureRequestOption(CaptureRequest.CONTROL_EFFECT_MODE,CaptureRequest.CONTROL_EFFECT_MODE_NEGATIVE);

        imageAnalysisBuilder.setTargetResolution(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST);

        ImageAnalysis imageAnalysis = imageAnalysisBuilder.build();

        imageAnalysis.setAnalyzer(exec, new ImageAnalysis.Analyzer() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void analyze(@NonNull ImageProxy image) {
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                TextResult[] results = null;
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                int nRowStride = image.getPlanes()[0].getRowStride();
                int nPixelStride = image.getPlanes()[0].getPixelStride();
                int length = buffer.remaining();
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                ImageData imageData = new ImageData(bytes, image.getWidth(), image.getHeight(), nRowStride * nPixelStride);
                try {
                    results = dbr.decodeBuffer(imageData.mBytes, imageData.mWidth, imageData.mHeight, imageData.mStride, EnumImagePixelFormat.IPF_NV21, "");
                } catch (BarcodeReaderException e) {
                    e.printStackTrace();
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Found ").append(results.length).append(" barcode(s):\n");
                for (int i = 0; i < results.length; i++) {
                    sb.append(results[i].barcodeText);
                    sb.append("\n");
                }
                Log.d("DBR", sb.toString());
                resultView.setText(sb.toString());
                image.close();
            }
        });

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(imageAnalysis)
                .build();
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, useCaseGroup);
    }

    private class ImageData {
        private int mWidth, mHeight, mStride;
        byte[] mBytes;

        ImageData(byte[] bytes, int nWidth, int nHeight, int nStride) {
            mBytes = bytes;
            mWidth = nWidth;
            mHeight = nHeight;
            mStride = nStride;
        }
    }
}
