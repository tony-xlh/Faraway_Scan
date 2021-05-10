package com.dynamsoft.farawayscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.ImageAnalysis;
import androidx.preference.PreferenceManager;

import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.BarcodeReaderException;
import com.dynamsoft.dbr.Point;
import com.dynamsoft.dbr.TextResult;
import com.dynamsoft.dbr.TextResultCallback;
import com.dynamsoft.dce.CameraEnhancer;
import com.dynamsoft.dce.CameraEnhancerException;
import com.dynamsoft.dce.CameraLTSLicenseVerificationListener;
import com.dynamsoft.dce.CameraListener;
import com.dynamsoft.dce.CameraState;
import com.dynamsoft.dce.CameraView;
import com.dynamsoft.dce.Frame;
import com.dynamsoft.dce.Resolution;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DCEActivity extends AppCompatActivity {
    private CameraEnhancer mCamera;
    private CameraView cameraView;
    private TextView resultView;
    private ImageView codeImageView;
    private ImageView srImageView;
    private BarcodeReader dbr;
    private SeekBar zoomRatioSeekBar;
    private Long TouchDownTime;
    private SharedPreferences prefs;
    private SuperResolution sr;
    private Boolean scanned;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dce);
        scanned=false;
        try {
            dbr = new BarcodeReader("t0068MgAAAJWPwDybm7nk0f9xYH25MMaVrZYcmhsiVoZrVo2hfcwRS74T6QA79OfzyvhC+9fgFI2noI8zBc66WHFCusVUgqk=");
        } catch (BarcodeReaderException barcodeReaderException) {
            barcodeReaderException.printStackTrace();
        }
        codeImageView = findViewById(R.id.codeImageView_dce);
        srImageView = findViewById(R.id.srImageView_dce);
        codeImageView.setImageBitmap(null);
        srImageView.setImageBitmap(null);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        sr = new SuperResolution(this);
        resultView=findViewById(R.id.resultView_dce);
        zoomRatioSeekBar = findViewById(R.id.zoomRatioSeekBar_dce);
        zoomRatioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Rect r = new Rect();
                float percent = progress/100;
                r.right=mCamera.getResolution().getWidth();
                r.bottom=mCamera.getResolution().getHeight();
                r.left= (int) (r.right*percent);
                r.top= (int) (r.bottom*percent);
                float factor = (float) percent*4;
                Log.d("DBR", String.valueOf(factor));
                mCamera.setZoomRegion(r,0);
                mCamera.setZoomFactor(factor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        initDCE();
    }

    private void initDCE(){
        cameraView = findViewById(R.id.cameraView);
        //Initialize your camera
        mCamera = new CameraEnhancer(this);
        com.dynamsoft.dce.DMLTSConnectionParameters info = new com.dynamsoft.dce.DMLTSConnectionParameters();
        // The organization id 200001 here will grant you a public trial license good for 7 days.
        // After that, you can send an email to trial@dynamsoft.com
        // (make sure to include the keyword privateTrial in the email title)
        // to obtain a 30-day free private trial license which will also come in the form of an organization id.
        info.organizationID = "200001";
        mCamera.initLicenseFromLTS(info,new CameraLTSLicenseVerificationListener() {
            @Override
            public void LTSLicenseVerificationCallback(boolean isSuccess, Exception error) {
                if(!isSuccess){
                    error.printStackTrace();
                }
            }
        });


        mCamera.addCameraView(cameraView);

        //Set camera on
        try {
            mCamera.setCameraDesiredState(CameraState.CAMERA_STATE_ON);
        } catch (CameraEnhancerException e) {
            e.printStackTrace();
        }
        mCamera.addCameraListener(new CameraListener() {
            @Override
            public void onPreviewOriginalFrame(Frame frame) {
                Log.d("DBR", "original");
                Log.d("DBR", "orientation: "+frame.getOrientation());
                Boolean continuous_scan = prefs.getBoolean("continuous_scan",false);
                if (scanned && continuous_scan==false){
                    return;
                }
                TextResult[] results = new TextResult[0];
                try {
                    results = dbr.decodeBuffer(frame.getData(),frame.getWidth(),frame.getHeight(),frame.getStrides()[0],frame.getFormat(),"");
                } catch (BarcodeReaderException e) {
                    e.printStackTrace();
                }

                Bitmap bitmap = FrameToBitmap(frame);

                if (results != null && results.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Found ").append(results.length).append(" barcode(s):\n");
                    if (results.length > 0) {
                        for (int j = 0; j < results.length; j++) {
                            TextResult tr = results[j];
                            sb.append(tr.barcodeText);
                            sb.append("\n");
                        }
                        Log.d("DBR", sb.toString());
                    } else {
                        Log.d("DBR", "No barcode found");
                    }
                    UpdateResult(sb.toString());
                    UpdateCodeImage(results[0].localizationResult.resultPoints,bitmap);
                    mCamera.setResultPoints(Utils.PointsAsArrayList(results[0].localizationResult.resultPoints));
                }else{
                    Point[] resultPoints = new Point[0];
                    try {
                        resultPoints = Utils.getResultsPointsWithHighestConfidence(dbr.getIntermediateResults());
                        if (resultPoints!=null){
                            mCamera.setResultPoints(Utils.PointsAsArrayList(resultPoints));
                        }
                    } catch (BarcodeReaderException e) {
                        e.printStackTrace();
                    }

                    if (prefs.getBoolean("superresolution", false) == true){
                        if (resultPoints!=null){
                            Log.d("DBR","run super resolution");
                            Bitmap cropped = Utils.DetecetdBarcodeZone(resultPoints,bitmap);
                            Bitmap srbm = sr.SuperResolutionImage(cropped);
                            try {
                                results = dbr.decodeBufferedImage(srbm, "");
                            } catch (BarcodeReaderException | IOException e) {
                                e.printStackTrace();
                            }
                            if (results.length>0){
                                UpdateCodeAndSRImage(cropped,srbm);
                            }
                        }
                    }
                }
                if (results.length==0){
                    cameraView.removeOverlay();
                    UpdateResult("No barcode found");
                } else{
                    cameraView.addOverlay();
                    scanned=true;
                }
            }
            @Override
            public void onPreviewFilterFrame(Frame frame) {
                Log.d("DBR", "filter");
            }

            @Override
            public void onPreviewFastFrame(Frame frame) {
                Log.d("DBR", "fastframe");
                Log.d("DBR", "orientation: "+frame.getOrientation());
            }
        });

        Resolution res = Resolution.DEFALUT;
        if (prefs.getString("resolution", "1080P").equals("1080P")) {
            res=Resolution.RESOLUTION_1080P;
        } else if (prefs.getString("resolution", "4K").equals("4K")){
            res=Resolution.RESOLUTION_4K;
        }

        mCamera.enableFastMode(true);
        mCamera.setResolution(res);
        mCamera.enableAutoZoom(prefs.getBoolean("autozoom", true));
        mCamera.startScanning();
    }

    private Bitmap FrameToBitmap(Frame frame){
        ByteArrayOutputStream outputSteam = new ByteArrayOutputStream();
        Rect rect = new Rect();
        rect.left=0;
        rect.top=0;
        rect.bottom=frame.height;
        rect.right=frame.width;
        YuvImage image = new YuvImage(frame.data, ImageFormat.NV21, frame.width, frame.height, frame.strides);
        image.compressToJpeg(rect,100,outputSteam);
        byte[] b = outputSteam.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
        return bitmap;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private double UpdateCodeImage(Point[] resultPoints,Bitmap bitmap){
        int minX,maxX,minY,maxY;
        minX=resultPoints[0].x;
        minY=resultPoints[0].y;
        maxX=0;
        maxY=0;
        for (Point point:resultPoints){
            minX=Math.min(point.x,minX);
            minY=Math.min(point.y,minY);
            maxX=Math.max(point.x,maxX);
            maxY=Math.max(point.y,maxY);
        }
        int width = maxX-minX;
        int height = maxY-minY;
        if (width<0 || height<0){
            return 0;
        }
        Bitmap cropped = Bitmap.createBitmap(bitmap, minX, minY, width, height);
        Bitmap rotated = Utils.rotatedBitmap(cropped,90); //default value
        double percent = Math.min((double) minX/bitmap.getWidth(),(double) minY/bitmap.getHeight());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                codeImageView.setImageBitmap(rotated);
            }
        });
        return percent;
    }

    private void UpdateResult(String result){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                resultView.setText(result);
            }
        });
    }

    private void UpdateCodeAndSRImage(Bitmap cropped,Bitmap sr){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                codeImageView.setImageBitmap(cropped);
                srImageView.setImageBitmap(sr);
            }
        });
    }
}