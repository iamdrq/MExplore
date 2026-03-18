package com.nd.me;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.davemorrissey.labs.subscaleview.decoder.DecoderFactory;
import com.nd.me.util.ByteArrayBitmapDecoder;
import com.nd.me.util.ByteArrayRegionDecoder;
import com.nd.me.util.StorageUtils;

public class ImageActivity extends AppCompatActivity {

    public class ByteArrayRegionDecoderFactory implements DecoderFactory<ByteArrayRegionDecoder> {
        private final byte[] imageData;

        public ByteArrayRegionDecoderFactory(byte[] imageData) {
            this.imageData = imageData;
        }

        @Override
        public ByteArrayRegionDecoder make() {
            return new ByteArrayRegionDecoder(imageData);
        }
    }

    public class ByteArrayBitmapDecoderFactory implements DecoderFactory<ByteArrayBitmapDecoder> {
        private final byte[] imageData;

        public ByteArrayBitmapDecoderFactory(byte[] imageData) {
            this.imageData = imageData;
        }

        @Override
        public ByteArrayBitmapDecoder make() {
            return new ByteArrayBitmapDecoder(imageData);
        }
    }

    private SubsamplingScaleImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        imageView = findViewById(R.id.imageView);
        //imageView.setImage(ImageSource.bitmap(StorageUtils.bitmapShare));
        imageView.setRegionDecoderFactory(new ByteArrayRegionDecoderFactory(StorageUtils.IMAGE_BYTE_SHARE));
        imageView.setBitmapDecoderFactory(new ByteArrayBitmapDecoderFactory(StorageUtils.IMAGE_BYTE_SHARE));
        imageView.setImage(ImageSource.uri("memory://image_data"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageView != null) {
            imageView.recycle();
        }
    }
}