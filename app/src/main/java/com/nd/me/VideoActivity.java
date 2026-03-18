package com.nd.me;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.google.android.material.slider.Slider;
import com.nd.me.util.StorageUtils;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoActivity extends AppCompatActivity {

    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    String currentUrl = null;

    Slider seekBar;

    ImageButton btnRotate, btnRepeat, btnPlay, btnBack, btnAudiotrack, btnSubtitles, btnScale;
    TextView timeCurrent, timeTotal, videoTitle;
    View topBar, bottomBar;
    Handler updateHandler = new Handler();

    private Runnable hideRunnable = () -> hideControls();
    private Runnable updateRunnable;
    private boolean controlsVisible = true;
    private Handler controlHandler = new Handler();
    private GestureDetector gestureDetector;

    private boolean userLockedOrientation = false;
    private boolean repeat = false;
    boolean wasPlaying = true;

    public enum OrientationMode {

        AUTO,
        USER_LAND,
        USER_PORT
    }

    private OrientationMode orientationMode = OrientationMode.AUTO;

    private void showControls() {

        topBar.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);

        controlsVisible = true;

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        resetHideTimer();
    }

    private void hideControls() {

        topBar.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        controlsVisible = false;
    }

    private void resetHideTimer() {
        if (StorageUtils.isMusic(videoTitle.getText().toString())) {
            return;
        }
        controlHandler.removeCallbacks(hideRunnable);
        controlHandler.postDelayed(hideRunnable, 3000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video);

        topBar = findViewById(R.id.top_bar);
        bottomBar = findViewById(R.id.bottom_bar);

        ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        seekBar = findViewById(R.id.seek_bar);

        currentUrl = getIntent().getStringExtra("url");

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);

        mMediaPlayer.setEventListener(event -> {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    mVideoLayout.setKeepScreenOn(true);
                    runOnUiThread(this::autoRotateByVideo);
                    break;

                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                case MediaPlayer.Event.EndReached:
                    mVideoLayout.setKeepScreenOn(false);
                    break;
                default:
                    break;
            }

            if (event.type == MediaPlayer.Event.EndReached) {
                runOnUiThread(() -> {
                    if (repeat) {
                        IMedia media = mMediaPlayer.getMedia();
                        if (media == null) return;

                        mMediaPlayer.stop();
                        mMediaPlayer.setMedia(media);
                        media.release();

                        mMediaPlayer.play();
                    } else {
                        finish();
                    }
                });
            }
        });

        mVideoLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {

                        if (controlsVisible) {
                            hideControls();
                        } else {
                            showControls();
                        }
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffX) > Math.abs(diffY)) {

                            if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                                if (diffX > 0) {
                                    seekBy(10_000);
                                } else {
                                    seekBy(-10_000);
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });

        try {
            final Media media = new Media(mLibVLC, Uri.parse(currentUrl));
            mMediaPlayer.setMedia(media);
            media.release();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        videoTitle = findViewById(R.id.video_title);
        String[] titles = currentUrl.split("/");
        if (titles.length > 1) {
            videoTitle.setText(titles[titles.length - 1]);
        }
        btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener((v) -> {
            this.finish();
        });


        timeCurrent = findViewById(R.id.time_current);
        timeTotal = findViewById(R.id.time_total);
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    long time = mMediaPlayer.getTime();
                    long length = mMediaPlayer.getLength();

                    if (time >= length) {
                        seekBar.setVisibility(View.INVISIBLE);
                    } else {
                        seekBar.setValueTo(length);
                        seekBar.setValue(time);
                    }

                    timeCurrent.setText(formatTime(time));
                    timeTotal.setText(formatTime(length));
                }
                updateHandler.postDelayed(this, 1000);
            }
        };

        seekBar.addOnChangeListener((slider, v, fromUser) -> {
            if (fromUser) {
                mMediaPlayer.setTime((long) v);
                resetHideTimer();
            }
        });
        seekBar.setLabelFormatter(value ->
                formatTime((long) value)
        );

        btnRotate = findViewById(R.id.btn_rotate);
        btnRotate.setOnClickListener(v -> {
            userLockedOrientation = true;
            setRequestedOrientation(
                    getRequestedOrientation() ==
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ?
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            );
        });
        btnRepeat = findViewById(R.id.btn_repeat);
        btnRepeat.setOnClickListener(v -> {
            repeat = !repeat;
            btnRepeat.setImageResource(repeat ? R.drawable.round_repeat_one_24 : R.drawable.round_repeat_24);
        });

        btnPlay = findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(v -> {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                btnPlay.setImageResource(R.drawable.round_play_arrow_24);
            } else {
                mMediaPlayer.play();
                btnPlay.setImageResource(R.drawable.round_pause_24);
            }
        });

        btnAudiotrack = findViewById(R.id.btn_audiotrack);
        btnAudiotrack.setOnClickListener(this::showAudioTrackMenu);
        btnSubtitles = findViewById(R.id.btn_subtitles);
        btnSubtitles.setOnClickListener(this::showSubtitleMenu);
        btnScale = findViewById(R.id.btn_scale);
        btnScale.setOnClickListener(v -> {
            List<MediaPlayer.ScaleType> mainScales = Arrays.asList(MediaPlayer.ScaleType.getMainScaleTypes());
            int currentIndex = mainScales.indexOf(mMediaPlayer.getVideoScale());
            int nextIndex = (currentIndex + 1) % mainScales.size();
            MediaPlayer.ScaleType nextScale = mainScales.get(nextIndex);
            setVideoScale(nextScale);
        });
    }

    private void showAudioTrackMenu(View anchor) {

        PopupMenu popupMenu = new PopupMenu(this, anchor);

        MediaPlayer.TrackDescription[] tracks =
                mMediaPlayer.getAudioTracks();

        int current = mMediaPlayer.getAudioTrack();

        if (tracks != null) {
            for (MediaPlayer.TrackDescription track : tracks) {
                String name = track.name != null ? track.name : "Audio " + track.id;
                popupMenu.getMenu()
                        .add(0, track.id, 0, name)
                        .setCheckable(true)
                        .setChecked(track.id == current);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            mMediaPlayer.setAudioTrack(item.getItemId());
            return true;
        });

        popupMenu.show();
    }

    private void showSubtitleMenu(View anchor) {

        PopupMenu popupMenu = new PopupMenu(this, anchor);

        popupMenu.getMenu()
                .add(0, 0, 0, getString(R.string.sel_outer_spu));

        MediaPlayer.TrackDescription[] tracks =
                mMediaPlayer.getSpuTracks();

        int current = mMediaPlayer.getSpuTrack();

        if (tracks != null) {
            for (MediaPlayer.TrackDescription track : tracks) {

                String name = track.name != null ? track.name : "Subtitle " + track.id;

                popupMenu.getMenu()
                        .add(1, track.id, 0, name)
                        .setCheckable(true)
                        .setChecked(track.id == current);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getGroupId() == 0) {
                Intent intent = new Intent(VideoActivity.this, ExploreActivity.class);
                intent.putExtra("url", this.currentUrl.replace(videoTitle.getText(),""));
                startActivityForResult(intent,1);
            } else if (item.getGroupId() == 1) {
                mMediaPlayer.setSpuTrack(item.getItemId());
            }
            return true;
        });

        popupMenu.show();
    }

    private void setVideoScale(MediaPlayer.ScaleType s) {
        mMediaPlayer.setVideoScale(s);
        switch (s) {
            case SURFACE_BEST_FIT:
                btnScale.setImageResource(R.drawable.aspect_ratio_24px);
                break;
            case SURFACE_FIT_SCREEN:
                btnScale.setImageResource(R.drawable.fit_screen_24px);
                break;
            case SURFACE_FILL:
                btnScale.setImageResource(R.drawable.fullscreen_24px);
                break;
            case SURFACE_16_9:
                btnScale.setImageResource(R.drawable.crop_16_9_24px);
                break;
            case SURFACE_4_3:
                btnScale.setImageResource(R.drawable.crop_4_3_24px);
                break;
            case SURFACE_ORIGINAL:
                btnScale.setImageResource(R.drawable.video_label_24px);
                break;
            default:
                break;
        }
    }

    private void seekBy(long deltaMs) {

        if (!mMediaPlayer.isSeekable()) return;

        long current = mMediaPlayer.getTime();
        long duration = mMediaPlayer.getLength();

        long newTime = current + deltaMs;

        if (newTime < 0) newTime = 0;
        if (newTime > duration) newTime = duration - 1000;

        mMediaPlayer.setTime(newTime);
    }

    private void autoRotateByVideo() {
        IMedia.VideoTrack videoTrack = mMediaPlayer.getCurrentVideoTrack();
        if (videoTrack == null) {
            return;
        }
        if (userLockedOrientation) {
            return;
        }
        int w = videoTrack.width;
        int h = videoTrack.height;

        if (w == 0 || h == 0) return;

        float ratio = (float) w / h;
        if (ratio > 1.0f) {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            );
        } else {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e("Life", "onStart");
        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);
        updateHandler.post(updateRunnable);
        showControls();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("Life", "onStop");
        updateHandler.removeCallbacks(updateRunnable);
        controlHandler.removeCallbacks(hideRunnable);
        mMediaPlayer.detachViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Life", "onPause");
        wasPlaying = mMediaPlayer.isPlaying();
        mMediaPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("Life", "onResume");
        if (wasPlaying) {
            mMediaPlayer.play();
            /*long pos = mMediaPlayer.getTime();
            if (pos > 0) {
                mMediaPlayer.setTime(pos);
            }*/
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateHandler.removeCallbacksAndMessages(null);
        controlHandler.removeCallbacksAndMessages(null);

        mMediaPlayer.stop();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}