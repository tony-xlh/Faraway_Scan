package com.dynamsoft.farawayscan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.BarcodeReaderException;
import com.dynamsoft.dbr.EnumIntermediateResultSavingMode;
import com.dynamsoft.dbr.EnumIntermediateResultType;
import com.dynamsoft.dbr.EnumResultCoordinateType;
import com.dynamsoft.dbr.Point;
import com.dynamsoft.dbr.PublicRuntimeSettings;
import com.dynamsoft.dbr.TextResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PictureActivity extends AppCompatActivity {
    private static  final int TAKE_PHOTO = 20;
    private static  final int LOAD_IMAGE = 10;
    private TextView resultTextView;
    private ImageView iv;
    private ImageView codeImageView;
    private ImageView srImageView;
    private BarcodeReader dbr;
    private Uri photoUri;
    private SuperResolution sr;
    private SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);
        resultTextView=findViewById(R.id.resultView2);
        resultTextView.setMovementMethod(new ScrollingMovementMethod());
        iv=findViewById(R.id.imageView);
        codeImageView=findViewById(R.id.codeImageView2);
        srImageView=findViewById(R.id.srImageView2);
        sr = new SuperResolution(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            dbr = new BarcodeReader("t0068MgAAAJWPwDybm7nk0f9xYH25MMaVrZYcmhsiVoZrVo2hfcwRS74T6QA79OfzyvhC+9fgFI2noI8zBc66WHFCusVUgqk=");
        } catch (BarcodeReaderException e) {
            e.printStackTrace();
        }

        try {
            PublicRuntimeSettings rs = dbr.getRuntimeSettings();
            rs.intermediateResultTypes= EnumIntermediateResultType.IRT_TYPED_BARCODE_ZONE;
            rs.intermediateResultSavingMode= EnumIntermediateResultSavingMode.IRSM_MEMORY;
            rs.resultCoordinateType= EnumResultCoordinateType.RCT_PIXEL;
            dbr.updateRuntimeSettings(rs);
        } catch (BarcodeReaderException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        codeImageView.setImageBitmap(null);
        srImageView.setImageBitmap(null);
        if (requestCode==TAKE_PHOTO){
            try {
                //Bitmap photo = (Bitmap) data.getExtras().get("data"); //The image is blurry.
                //iv.setImageBitmap(photo);
                if (photoUri != null) {
                    iv.setImageURI(photoUri);
                    Bitmap bm =  ((BitmapDrawable)iv.getDrawable()).getBitmap();
                    decodeBitmap(bm,true);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        } else if (requestCode==LOAD_IMAGE){
            Uri imageUri = data.getData();
            iv.setImageURI(imageUri);
            Bitmap bm =  ((BitmapDrawable)iv.getDrawable()).getBitmap();
            decodeBitmap(bm,true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void takePhoto(View view) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(this.getPackageManager()) != null) {
            File file = null;
            try {
                file = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            photoUri = null;
            if (file != null) {
                photoUri = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("pic", ".jpg", storageDir);
    }

    public void loadImage(View view) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, LOAD_IMAGE);
        } else{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent,LOAD_IMAGE);
        }
    }

    public void decodeBitmap(Bitmap bm,Boolean original)  {
        TextResult[] results = new TextResult[0];
        try {
            results = dbr.decodeBufferedImage(bm,"");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BarcodeReaderException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.length).append(" barcode(s):\n");
        for (int i = 0; i < results.length; i++) {
            sb.append(results[i].barcodeText);
            sb.append("\n");
        }
        Log.d("DBR",sb.toString());
        resultTextView.setText(sb.toString());

        if (results.length>0){
            if (original){
                UpdateCodeImage(results[0].localizationResult.resultPoints,bm);
            }
        } else{
            Point[] resultPoints = null;
            try {
                resultPoints = Utils.getResultsPointsWithHighestConfidence(dbr.getIntermediateResults());
            } catch (BarcodeReaderException e) {
                e.printStackTrace();
            }
            if (resultPoints!=null){
                if (original){
                    UpdateCodeImage(resultPoints,bm);
                }
            }else{
                Log.d("DBR","no detected zones");
            }

            if (original && prefs.getBoolean("superresolution", false) == true){
                if (resultPoints==null){
                    trySr(iv);
                }else{
                    trySr(codeImageView);
                }
            }
        }

    }

    private double UpdateCodeImage(Point[] resultPoints, Bitmap bitmap){
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
        double percent = Math.min((double) minX/bitmap.getWidth(),(double) minY/bitmap.getHeight());
        codeImageView.setImageBitmap(cropped);
        return percent;
    }

    private void trySr(ImageView imageview){
        Log.d("DBR","run super resolution");
        Bitmap bm =  ((BitmapDrawable)imageview.getDrawable()).getBitmap();
        Bitmap srbm = sr.SuperResolutionImage(bm);
        srImageView.setImageBitmap(srbm);
        decodeBitmap(srbm,false);
    }
}