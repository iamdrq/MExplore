package com.nd.me.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.davemorrissey.labs.subscaleview.decoder.ImageDecoder;

public class ByteArrayBitmapDecoder implements ImageDecoder {
    private final byte[] imageData;

    public ByteArrayBitmapDecoder(byte[] imageData) {
        this.imageData = imageData;
    }

    @Override
    public Bitmap decode(Context context, Uri uri) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        if (bitmap == null) {
            throw new RuntimeException("Small image decoding failed");
        }
        return bitmap;
    }
}
