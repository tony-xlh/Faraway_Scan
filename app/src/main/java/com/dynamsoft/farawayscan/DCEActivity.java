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
    private Context ctx;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dce);
        scanned=false;
        ctx=this;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            dbr = new BarcodeReader("t0068MgAAAJWPwDybm7nk0f9xYH25MMaVrZYcmhsiVoZrVo2hfcwRS74T6QA79OfzyvhC+9fgFI2noI8zBc66WHFCusVUgqk=");
            Utils.updateDBRSettings(dbr,prefs);
        } catch (BarcodeReaderException barcodeReaderException) {
            barcodeReaderException.printStackTrace();
        }
        codeImageView = findViewById(R.id.codeImageView_dce);
        srImageView = findViewById(R.id.srImageView_dce);
        codeImageView.setImageBitmap(null);
        srImageView.setImageBitmap(null);
        sr = new SuperResolution(this);
        resultView=findViewById(R.id.resultView_dce);
        zoomRatioSeekBar = findViewById(R.id.zoomRatioSeekBar_dce);
        zoomRatioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float percent = progress/100;
                float factor = (float) percent*4;
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
                Bitmap bitmap = FrameToBitmap(frame);
                Boolean continuous_scan = prefs.getBoolean("continuous_scan",false);
                if (scanned && continuous_scan==false){
                    return;
                }
                String resultString="";
                TextResult[] results = new TextResult[0];
                try {
                    results = dbr.decodeBufferedImage(bitmap, "");
                    //results = dbr.decodeBuffer(frame.getData(),frame.getWidth(),frame.getHeight(),frame.getStrides()[0],frame.getFormat(),"");
                } catch (BarcodeReaderException | IOException e) {
                    e.printStackTrace();
                }

                if (results != null && results.length > 0) {
                    if (results.length > 0) {
                        resultString=Utils.getBarcodeResult(results);
                        UpdateCodeImage(results[0].localizationResult.resultPoints,bitmap);
                        mCamera.setResultPoints(Utils.PointsAsArrayList(results[0].localizationResult.resultPoints));
                    }
                }else{
                    Point[] resultPoints = new Point[0];
                    try {
                        resultPoints = Utils.getResultsPointsWithHighestConfidence(dbr.getIntermediateResults());
                        Log.d("DBR", "result points: "+resultPoints);
                        if (resultPoints!=null){
                            Log.d("DBR", "autozoom");
                            mCamera.setResultPoints(Utils.PointsAsArrayList(resultPoints)); //autozoom
                            mCamera.setZoomRegion(GetRect(resultPoints),frame.getOrientation());
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
                                if (results.length>0){
                                    resultString=Utils.getBarcodeResult(results);
                                    Utils.saveRecord(resultString,cropped,srbm,ctx,prefs);
                                    UpdateCodeAndSRImage(cropped,srbm);
                                }
                            } catch (BarcodeReaderException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (results.length==0){
                    cameraView.removeOverlay();
                    UpdateResult("No barcode found");
                    Log.d("DBR", "No barcode found");
                } else{
                    cameraView.addOverlay();
                    UpdateResult(resultString);
                    scanned=true;
                    if (prefs.getBoolean("save_only_superresolution", false) == false){
                        try {
                            Utils.saveRecord(resultString,bitmap,null,ctx,prefs);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (continuous_scan==false){
                        mCamera.pauseCamera();
                    }
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
        mCamera.enableAutoZoom(prefs.getBoolean("autozoom", true));
        mCamera.setResolution(res);
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
        if (scanned && prefs.getBoolean("continuous_scan",false)==false){
            mCamera.resumeCamera();
            scanned=false;
        }else{
            super.onBackPressed();
        }

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

    private Rect GetRect(Point[] points) {
        int leftX = (points[0]).x, rightX = leftX;
        int leftY = (points[0]).y, rightY = leftY;
        for (Point pt : points) {
            if (pt.x < leftX)
                leftX = pt.x;
            if (pt.y < leftY)
                leftY = pt.y;
            if (pt.x > rightX)
                rightX = pt.x;
            if (pt.y > rightY)
                rightY = pt.y;
        }
        Rect frameRegion = new Rect(leftX, leftY, rightX, rightY);
        return frameRegion;
    }
}