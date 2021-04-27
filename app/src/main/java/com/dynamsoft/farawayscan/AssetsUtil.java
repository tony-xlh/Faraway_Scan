package com.dynamsoft.farawayscan;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import com.google.common.io.ByteStreams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.content.res.AssetManager.ACCESS_BUFFER;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

/** Helper to load assets. */
class AssetsUtil {

    private AssetsUtil() {}

    /**
     * Gets AssetFileDescriptor directly for given a path, or returns its copy by caching for the
     * compressed one.
     */
    public static AssetFileDescriptor getAssetFileDescriptorOrCached(
            Context context, String assetPath) throws IOException {
        try {
            return context.getAssets().openFd(assetPath);
        } catch (FileNotFoundException e) {
            // If it cannot read from asset file (probably compressed), try copying to cache folder and
            // reloading.
            File cacheFile = new File(context.getCacheDir(), assetPath);
            cacheFile.getParentFile().mkdirs();
            copyToCacheFile(context, assetPath, cacheFile);
            ParcelFileDescriptor cachedFd = ParcelFileDescriptor.open(cacheFile, MODE_READ_ONLY);
            return new AssetFileDescriptor(cachedFd, 0, cacheFile.length());
        }
    }

    private static void copyToCacheFile(Context context, String assetPath, File cacheFile)
            throws IOException {
        try (InputStream inputStream = context.getAssets().open(assetPath, ACCESS_BUFFER);
             FileOutputStream fileOutputStream = new FileOutputStream(cacheFile, false)) {
            ByteStreams.copy(inputStream, fileOutputStream);
        }
    }
}
