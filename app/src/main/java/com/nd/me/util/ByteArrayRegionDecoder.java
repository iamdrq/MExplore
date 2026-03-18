package com.nd.me.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;

import com.davemorrissey.labs.subscaleview.decoder.ImageRegionDecoder;

import java.io.IOException;

public class ByteArrayRegionDecoder implements ImageRegionDecoder {
    private final byte[] imageData;
    private BitmapRegionDecoder decoder;
    private final Object decoderLock = new Object();

    public ByteArrayRegionDecoder(byte[] imageData) {
        this.imageData = imageData;
    }

    @Override
    public Point init(Context context, Uri uri) throws Exception {
        synchronized (decoderLock) {
            decoder = BitmapRegionDecoder.newInstance(imageData, 0, imageData.length, false);
            if (decoder == null) {
                throw new IOException("Failed to create BitmapRegionDecoder");
            }
            return new Point(decoder.getWidth(), decoder.getHeight());
        }
    }

    @Override
    public Bitmap decodeRegion(Rect sRect, int sampleSize) {
        synchronized (decoderLock) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            /*// RGB_565 reduce half memory(no alpha)
            options.inPreferredConfig = Bitmap.Config.RGB_565;*/

            Bitmap bitmap = decoder.decodeRegion(sRect, options);
            if (bitmap == null) {
                throw new RuntimeException("Region decoding failed");
            }
            return bitmap;
        }
    }

    @Override
    public boolean isReady() {
        return decoder != null && !decoder.isRecycled();
    }

    @Override
    public void recycle() {
        synchronized (decoderLock) {
            if (decoder != null) {
                decoder.recycle();
                decoder = null;
            }
        }
    }
}