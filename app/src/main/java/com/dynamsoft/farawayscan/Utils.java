package com.dynamsoft.farawayscan;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.WorkerThread;

import com.dynamsoft.dbr.BarcodeReader;
import com.dynamsoft.dbr.BarcodeReaderException;
import com.dynamsoft.dbr.EnumConflictMode;
import com.dynamsoft.dbr.EnumIntermediateResultSavingMode;
import com.dynamsoft.dbr.EnumIntermediateResultType;
import com.dynamsoft.dbr.EnumResultCoordinateType;
import com.dynamsoft.dbr.EnumScaleUpMode;
import com.dynamsoft.dbr.IntermediateResult;
import com.dynamsoft.dbr.LocalizationResult;
import com.dynamsoft.dbr.Point;
import com.dynamsoft.dbr.PublicRuntimeSettings;
import com.dynamsoft.dbr.TextResult;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Utils {

    public static void updateDBRSettings(BarcodeReader dbr, SharedPreferences prefs) throws BarcodeReaderException {
        String template = prefs.getString("template","");
        if (template.trim()!=""){
            dbr.initRuntimeSettingsWithString(template, EnumConflictMode.CM_OVERWRITE);
        }

        String scaleup = prefs.getString("scaleup","");
        Boolean scaleupEnabled = false;
        if (scaleup.split(",").length==3) {
            scaleupEnabled=true;
        }

        PublicRuntimeSettings rs = dbr.getRuntimeSettings();
        rs.intermediateResultTypes= EnumIntermediateResultType.IRT_TYPED_BARCODE_ZONE;
        rs.intermediateResultSavingMode= EnumIntermediateResultSavingMode.IRSM_MEMORY;
        rs.resultCoordinateType= EnumResultCoordinateType.RCT_PIXEL;

        if (scaleupEnabled){
            rs.scaleUpModes[1] = Integer.parseInt(scaleup.split(",")[0]);
        }
        
        dbr.updateRuntimeSettings(rs);
        if (scaleupEnabled) {
            Log.d("DBR","scaleup enabled");
            dbr.setModeArgument("ScaleUpModes",1,"ModuleSizeThreshold",scaleup.split(",")[1]);
            dbr.setModeArgument("ScaleUpModes",1,"TargetModuleSize",scaleup.split(",")[2]);
        }
    }

    public static Point[] getResultsPointsWithHighestConfidence(IntermediateResult[] intermediateResults){
        for (IntermediateResult ir:intermediateResults){
            if (ir.resultType== EnumIntermediateResultType.IRT_TYPED_BARCODE_ZONE){
                int maxConfidence=0;
                for (Object result:ir.results)
                {
                    LocalizationResult lr = (LocalizationResult) result;
                    maxConfidence=Math.max(lr.confidence,maxConfidence);
                    Log.d("DBR", "confidence: "+lr.confidence);
                }
                Log.d("DBR", "max confidence: "+maxConfidence);
                for (Object result:ir.results)
                {
                    LocalizationResult lr = (LocalizationResult) result;
                    if (lr.confidence==maxConfidence && maxConfidence>80){
                        return lr.resultPoints;
                    }
                }
            }
        }
        return null;
    }

    public static ArrayList<android.graphics.Point> GetResultPointsArrayListFromTextResults(TextResult[] results){
        ArrayList<android.graphics.Point> array = new ArrayList<>();
        for (TextResult result:results){
            for (Point point:result.localizationResult.resultPoints){
                android.graphics.Point newPoint = new android.graphics.Point();
                newPoint.x=point.x;
                newPoint.y=point.y;
                array.add(newPoint);
            }
        }
        return array;
    }

    public static ArrayList<android.graphics.Point> PointsAsArrayList(Point[] points){
        ArrayList<android.graphics.Point> array = new ArrayList<>();
        for (Point point:points){
            android.graphics.Point newPoint = new android.graphics.Point();
            newPoint.x=point.x;
            newPoint.y=point.y;
            array.add(newPoint);
        }
        return array;
    }

    public static Bitmap DetecetdBarcodeZone(Point[] resultPoints, Bitmap bitmap){
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
            return bitmap;
        }
        Bitmap cropped = Bitmap.createBitmap(bitmap, minX, minY, width, height);
        return cropped;
    }

    public static Bitmap rotatedBitmap(Bitmap bitmap, int rotationDegrees) {
        Matrix m = new Matrix();
        m.postRotate(rotationDegrees);
        Bitmap bitmapRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
        return bitmapRotated;
    }

    //Leave sr to null if not exist
    public static void saveRecord(String result, Bitmap image, Bitmap sr, Context ctx,SharedPreferences prefs) throws IOException {
        if (image != null) {
            Long timestamp = System.currentTimeMillis();
            File path = ctx.getExternalFilesDir(null);
            File imgfile = new File(path, timestamp + ".jpg");
            File srfile = new File(path, timestamp + "-sr.jpg");
            File txtfile = new File(path, timestamp + ".txt");
            String image_quality_str = prefs.getString("image_quality", "100");
            int image_quality = Integer.parseInt(image_quality_str);
            Log.d("DBR", imgfile.getAbsolutePath());
            Boolean save_image = prefs.getBoolean("save_image", false);
            if (save_image) {
                FileOutputStream outStream = new FileOutputStream(imgfile);
                image.compress(Bitmap.CompressFormat.JPEG, image_quality, outStream);
                outStream.close();
                if (sr!=null){
                    FileOutputStream srStream = new FileOutputStream(srfile);
                    sr.compress(Bitmap.CompressFormat.JPEG, image_quality, srStream);
                    srStream.close();
                }
            }
            FileOutputStream outStream2 = new FileOutputStream(txtfile);
            outStream2.write(result.getBytes(Charset.defaultCharset()));
            outStream2.close();
        }
    }

    public static String getBarcodeResult(TextResult[] results){
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.length).append(" barcode(s):\n");
        if (results.length>0){
            for (int i = 0; i < results.length; i++) {
                sb.append(results[i].barcodeText);
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
