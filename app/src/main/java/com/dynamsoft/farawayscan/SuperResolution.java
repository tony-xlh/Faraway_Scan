package com.dynamsoft.farawayscan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
        if (bm.getWidth()<50){
            bm=padded(bm);
        }else{
            bm = Bitmap.createScaledBitmap(bm, 50, 50, true);
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
        Bitmap bm = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        c.drawBitmap(code,0,0,paint);
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
