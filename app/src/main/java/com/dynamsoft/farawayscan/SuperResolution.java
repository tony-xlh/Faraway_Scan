package com.dynamsoft.farawayscan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

import androidx.annotation.WorkerThread;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class SuperResolution {
    private long superResolutionNativeHandle = 0;
    private MappedByteBuffer model;
    private static final String MODEL_NAME = "ESRGAN.tflite";
    private static final int UPSCALE_FACTOR = 4;
    private int inpwidth=50;
    private int inpheight=50;
    private Context context;
    static {
        System.loadLibrary("SuperResolution");
    }
    public SuperResolution(Context context) {
        this.context=context;
        superResolutionNativeHandle = initTFLiteInterpreter(false);
    }

    private long initTFLiteInterpreter(boolean useGPU) {
        try {
            model = loadModelFile();
        } catch (IOException e) {
            Log.e("SR", "Fail to load model", e);
        }
        return initWithByteBufferFromJNI(model, useGPU);
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        try (AssetFileDescriptor fileDescriptor =
                     AssetsUtil.getAssetFileDescriptorOrCached(context, MODEL_NAME);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public Bitmap SuperResolutionImage(Bitmap bm){
        if (bm.getWidth()<inpwidth && bm.getHeight()<inpheight){
            bm = padded(bm);
        }else{
            bm = squared(bm);
        }

        int[] lowResRGB = new int[bm.getWidth() * bm.getHeight()];
        bm.getPixels(
                lowResRGB, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
        int[] superResRGB = doSuperResolution(lowResRGB);
        Bitmap srImgBitmap =
                Bitmap.createBitmap(
                        superResRGB, bm.getWidth()*UPSCALE_FACTOR, bm.getHeight()*UPSCALE_FACTOR, Bitmap.Config.ARGB_8888);
        return srImgBitmap;
    }

    private Bitmap padded(Bitmap code) {
        int width = code.getWidth();
        int height = code.getHeight();
        Bitmap bm = Bitmap.createBitmap(inpwidth, inpheight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint bc = new Paint();
        bc.setColor(Color.WHITE);
        bc.setStyle(Paint.Style.FILL);
        Rect r = new Rect(0,0,width,height);
        c.drawRect(r,bc);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        c.drawBitmap(code,0,0,paint);
        return bm;
    }

    private Bitmap squared(Bitmap code) {
        int width = code.getWidth();
        int height = code.getHeight();
        int longside = Math.max(width,height);
        Bitmap bm = Bitmap.createBitmap(longside, longside, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint bc = new Paint();
        bc.setColor(Color.WHITE);
        bc.setStyle(Paint.Style.FILL);
        Rect r = new Rect(0,0,width,height);
        c.drawRect(r,bc);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        c.drawBitmap(code,0,0,paint);
        bm = Bitmap.createScaledBitmap(bm, inpwidth, inpheight, true);
        return bm;
    }

    @WorkerThread
    public synchronized int[] doSuperResolution(int[] lowResRGB) {
        return superResolutionFromJNI(superResolutionNativeHandle, lowResRGB);
    }

    private void deinit() {
        deinitFromJNI(superResolutionNativeHandle);
    }

    private native int[] superResolutionFromJNI(long superResolutionNativeHandle, int[] lowResRGB);

    private native long initWithByteBufferFromJNI(MappedByteBuffer modelBuffer, boolean useGPU);

    private native void deinitFromJNI(long superResolutionNativeHandle);
}
