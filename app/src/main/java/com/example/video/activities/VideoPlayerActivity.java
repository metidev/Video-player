package com.example.video.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.PictureInPictureParams;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Rational;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bullhead.equalizer.EqualizerFragment;
import com.bullhead.equalizer.Settings;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.example.video.modals.BrightnessDialog;
import com.example.video.modals.IconModal;
import com.example.video.modals.MediaFiles;
import com.example.video.modals.OnSwipeTouchListener;
import com.example.video.adapter.PlaybackIconsAdapter;
import com.example.video.modals.PlaylistDialog;
import com.example.video.R;
import com.example.video.modals.Utility;
import com.example.video.adapter.VideoFileAdapter;
import com.example.video.modals.VolumeDialog;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class VideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {
    ArrayList<MediaFiles> mVideoFiles = new ArrayList<>();
    PlayerView playerView;
    SimpleExoPlayer player;
    int position;
    String videoTitle;
    TextView title;
    private ControlsMode controlsMode;

    public enum ControlsMode {
        LOCK, FULLSCREEN
    }

    ImageView nextButton, preButton, lock, unlock, scaling, videoList, video_more;
    VideoFileAdapter videoFileAdapter;
    RelativeLayout root;
    ConcatenatingMediaSource concatenatingMediaSource;
    private ArrayList<IconModal> iconModalArrayList = new ArrayList<>();
    PlaybackIconsAdapter playbackIconsAdapter;
    RecyclerView recyclerViewIcons;
    Boolean expand = false, dark = false, mute = false;
    View nightMode;
    PlaybackParameters parameters;
    float speed;
    DialogProperties dialogProperties;
    FilePickerDialog filePickerDialog;
    Uri uriSubtitle;
    PictureInPictureParams.Builder pictureInPicture;
    boolean isCrossChecked;
    FrameLayout eqControl;
    private int device_height, device_width, brightness, media_volume;
    boolean start = false, left, right, success = false;
    private float baseX, baseY;
    boolean swipe_move = false;
    private long diffX, diffY;
    public static final int MINIMUM_DISTANSE = 100;
    TextView vol_text, brt_text, total_duration;
    ProgressBar volProgress, brtProgress;
    LinearLayout vol_progress_container, vol_text_container, brt_progress_container, brt_text_container;
    ImageView vol_icon, brt_icon;
    AudioManager audioManager;
    private ContentResolver contentResolver;
    private Window window;
    boolean singleTap = false;
    RelativeLayout zoomLayout, zoomContainer;
    TextView zoom_perc;
    ScaleGestureDetector scaleGestureDetector;
    private float scale_factor = 1.0f;
    boolean double_tap = false;
    RelativeLayout double_tap_playpause;


    @SuppressLint({"MissingInflatedId", "NotifyDataSetChanged"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_video_player);
        getSupportActionBar().hide();
        hideBottomBar();
        playerView = findViewById(R.id.exoplayer_view);
        position = getIntent().getIntExtra("position", 1);
        videoTitle = getIntent().getStringExtra("video_title");
        mVideoFiles = getIntent().getExtras().getParcelableArrayList("videoArrayList");
//        screenOrientation();

        findViewById(R.id.video_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player != null) {
                    player.release();
                }
                finish();
            }
        });

        initViews();
        playVideo();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        device_width = displayMetrics.widthPixels;
        device_height = displayMetrics.heightPixels;

        playerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @SuppressLint("SetTextI18n")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        playerView.showController();
                        start = true;
                        if (motionEvent.getX() < (device_width / 2)) {
                            left = true;
                            right = false;
                        } else if (motionEvent.getX() > (device_width / 2)) {
                            left = false;
                            right = true;
                        }
                        baseX = motionEvent.getX();
                        baseY = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        swipe_move = true;
                        diffX = (long) Math.ceil(motionEvent.getX() - baseX);
                        diffY = (long) Math.ceil(motionEvent.getY() - baseY);
                        double brightnessSpeed = 0.01;
                        if (Math.abs(diffY) > MINIMUM_DISTANSE) {
                            start = true;
                            if (Math.abs(diffY) > Math.abs(diffX)) {
                                boolean value = false;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    value = android.provider.Settings.System.canWrite(getApplicationContext());
                                    if (value) {
                                        if (left) {
                                            contentResolver = getContentResolver();
                                            window = getWindow();
                                            try {
                                                android.provider.Settings.System.putInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                        android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                                                brightness = android.provider.Settings.System.getInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS);
                                            } catch (
                                                    android.provider.Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                            int new_brightness = (int) (brightness - (diffY * brightnessSpeed));
                                            if (new_brightness > 250) {
                                                new_brightness = 250;
                                            } else if (new_brightness < 1) {
                                                new_brightness = 1;
                                            }
                                            double brt_percentage = Math.ceil((((double) new_brightness / (double) 250) * (double) 100));
                                            brt_progress_container.setVisibility(View.VISIBLE);
                                            brt_text_container.setVisibility(View.VISIBLE);
                                            brtProgress.setProgress((int) brt_percentage);

                                            if (brt_percentage < 30) {
                                                brt_icon.setImageResource(R.drawable.round_brightness_5_24);
                                            } else if (brt_percentage > 30 && brt_percentage < 80) {
                                                brt_icon.setImageResource(R.drawable.round_brightness_medium_24);
                                            } else if (brt_percentage > 80) {
                                                brt_icon.setImageResource(R.drawable.round_brightness_high_24);
                                            }

                                            brt_text.setText(" " + (int) brt_percentage + "%");
                                            android.provider.Settings.System.putInt(contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS,
                                                    (new_brightness));
                                            WindowManager.LayoutParams layoutParams = window.getAttributes();
                                            layoutParams.screenBrightness = brightness / (float) 255;
                                            window.setAttributes(layoutParams);
                                        } else if (right) {
                                            vol_text_container.setVisibility(View.VISIBLE);
                                            media_volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                            double cal = (double) diffY * ((double) maxVol / ((double) (device_height * 2) - brightnessSpeed));
                                            int newMediaVolume = media_volume - (int) cal;
                                            if (newMediaVolume > maxVol) {
                                                newMediaVolume = maxVol;
                                            } else if (newMediaVolume < 1) {
                                                newMediaVolume = 0;
                                            }
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newMediaVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                                            double volPer = Math.ceil((((double) newMediaVolume / (double) maxVol) * (double) 100));
                                            vol_text.setText(" " + (int) volPer);
                                            if (volPer < 1) {
                                                vol_icon.setImageResource(R.drawable.round_volume_off_24);
                                                vol_text.setVisibility(View.VISIBLE);
                                                vol_text.setText("off");
                                            } else if (volPer >= 1) {
                                                vol_icon.setImageResource(R.drawable.round_volume_up_24);
                                                vol_text.setVisibility(View.VISIBLE);
                                            }
                                            vol_progress_container.setVisibility(View.VISIBLE);
                                            volProgress.setProgress((int) volPer);
                                        }
                                        success = true;
                                    } else {
                                        Toasty.warning(VideoPlayerActivity.this, getString(R.string.allow_write_settings_for_swipe_controls), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                        intent.setData(Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 111);
                                    }
                                }
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        swipe_move = false;
                        start = false;
                        vol_progress_container.setVisibility(View.GONE);
                        brt_progress_container.setVisibility(View.GONE);
                        vol_text_container.setVisibility(View.GONE);
                        brt_text_container.setVisibility(View.GONE);
                        break;
                }
                scaleGestureDetector.onTouchEvent(motionEvent);
                return super.onTouch(view, motionEvent);
            }

            @Override
            public void onDoubleTouch() {
                super.onDoubleTouch();
                if (double_tap) {
                    player.setPlayWhenReady(true);
                    double_tap_playpause.setVisibility(View.GONE);
                    double_tap = false;
                } else {
                    player.setPlayWhenReady(false);
                    double_tap_playpause.setVisibility(View.VISIBLE);
                    double_tap = true;
                }

            }

            @Override
            public void onSingleTouch() {
                super.onSingleTouch();
                if (singleTap) {
                    playerView.showController();
                    singleTap = false;
                } else {
                    playerView.hideController();
                    singleTap = true;
                }
                if (double_tap_playpause.getVisibility() == View.VISIBLE) {
                    double_tap_playpause.setVisibility(View.GONE);
                }
            }
        });
        horizontaliconList();
    }

    private void horizontaliconList() {
        iconModalArrayList.add(new IconModal(R.drawable.round_keyboard_arrow_right_24, ""));
        iconModalArrayList.add(new IconModal(R.drawable.round_nights_stay_24, getString(R.string.night)));
        iconModalArrayList.add(new IconModal(R.drawable.round_picture_in_picture_alt_24, getString(R.string.popup)));
        iconModalArrayList.add(new IconModal(R.drawable.round_volume_off_24, getString(R.string.mute)));
        iconModalArrayList.add(new IconModal(R.drawable.round_screen_rotation_24, getString(R.string.rotate)));

        playbackIconsAdapter = new PlaybackIconsAdapter(iconModalArrayList, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this
                , LinearLayoutManager.HORIZONTAL, true);
        recyclerViewIcons.setLayoutManager(layoutManager);
        recyclerViewIcons.setAdapter(playbackIconsAdapter);
        playbackIconsAdapter.notifyDataSetChanged();
        playbackIconsAdapter.setOnItemClickListener(new PlaybackIconsAdapter.OnItemClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onItemClick(int position) {
                if (position == 0) {
                    if (expand) {
                        iconModalArrayList.clear();
                        iconModalArrayList.add(new IconModal(R.drawable.round_keyboard_arrow_right_24, ""));
                        iconModalArrayList.add(new IconModal(R.drawable.round_nights_stay_24, getString(R.string.night)));
                        iconModalArrayList.add(new IconModal(R.drawable.round_picture_in_picture_alt_24, getString(R.string.popup)));
                        iconModalArrayList.add(new IconModal(R.drawable.round_volume_off_24, getString(R.string.mute)));
                        iconModalArrayList.add(new IconModal(R.drawable.round_screen_rotation_24, getString(R.string.rotate)));
                        playbackIconsAdapter.notifyDataSetChanged();

                        expand = false;
                    } else {
                        if (iconModalArrayList.size() == 5) {
                            iconModalArrayList.add(new IconModal(R.drawable.round_volume_up_24, getString(R.string.volume)));
                            iconModalArrayList.add(new IconModal(R.drawable.round_brightness_medium_24, getString(R.string.brightness)));
                            iconModalArrayList.add(new IconModal(R.drawable.round_equalizer_24, getString(R.string.equalizer)));
                            iconModalArrayList.add(new IconModal(R.drawable.round_fast_forward_24, getString(R.string.speed)));
                            iconModalArrayList.add(new IconModal(R.drawable.round_subtitles_24, getString(R.string.subtitle)));
                        }
                        iconModalArrayList.set(position, new IconModal(R.drawable.round_keyboard_arrow_left_24, ""));
                        playbackIconsAdapter.notifyDataSetChanged();

                        expand = true;
                    }
                }
                if (position == 1) {
                    if (dark) {
                        nightMode.setVisibility(View.GONE);
                        iconModalArrayList.set(position, new IconModal(R.drawable.round_nights_stay_24, getString(R.string.night)));
                        playbackIconsAdapter.notifyDataSetChanged();
                        dark = false;
                    } else {
                        nightMode.setVisibility(View.VISIBLE);
                        iconModalArrayList.set(position, new IconModal(R.drawable.round_nights_stay_24, getString(R.string.day)));
                        playbackIconsAdapter.notifyDataSetChanged();
                        dark = true;
                    }
                }
                if (position == 2) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Rational aspectRational = new Rational(16, 9);
                        pictureInPicture.setAspectRatio(aspectRational);
                        enterPictureInPictureMode(pictureInPicture.build());
                    } else {
                        Log.wtf("not oreo", "yes");
                    }
                }
                if (position == 3) {
                    if (mute) {
                        player.setVolume(100);
                        iconModalArrayList.set(position, new IconModal(R.drawable.round_volume_off_24, getString(R.string.mute)));
                        playbackIconsAdapter.notifyDataSetChanged();
                        mute = false;
                    } else {
                        player.setVolume(100);
                        iconModalArrayList.set(position, new IconModal(R.drawable.round_volume_up_24, getString(R.string.unmute)));
                        playbackIconsAdapter.notifyDataSetChanged();
                        mute = true;
                    }
                }
                if (position == 4) {
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        playbackIconsAdapter.notifyDataSetChanged();
                    } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        playbackIconsAdapter.notifyDataSetChanged();
                    }
                }
                if (position == 5) {
                    VolumeDialog volumeDialog = new VolumeDialog();
                    volumeDialog.show(getSupportFragmentManager(), "dialog");
                    playbackIconsAdapter.notifyDataSetChanged();
                }
                if (position == 6) {
                    BrightnessDialog brightnessDialog = new BrightnessDialog();
                    brightnessDialog.show(getSupportFragmentManager(), "dialog");
                    playbackIconsAdapter.notifyDataSetChanged();
                }
                if (position == 7) {
                    if (eqControl.getVisibility() == View.GONE) {
                        eqControl.setVisibility(View.VISIBLE);
                    }
                    final int sessionId = player.getAudioSessionId();
                    Settings.isEditing = false;
                    EqualizerFragment equalizerFragment = EqualizerFragment.newBuilder()
                            .setAccentColor(Color.parseColor("#1A78F2"))
                            .setAudioSessionId(sessionId).build();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.eqFrame, equalizerFragment)
                            .commit();
                    playbackIconsAdapter.notifyDataSetChanged();

                }
                if (position == 8) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(VideoPlayerActivity.this);
                    alertDialog.setTitle(getString(R.string.select_playback_speed)).setPositiveButton(R.string.ok, null);
                    String[] items = {"0.5x", "1x Normal", "1.25x", "1.5x", "2x"};
                    int checkItem = -1;
                    alertDialog.setSingleChoiceItems(items, checkItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            switch (i) {
                                case 0:
                                    speed = 0.5f;
                                    parameters = new PlaybackParameters(speed);
                                    player.setPlaybackParameters(parameters);
                                    break;
                                case 1:
                                    speed = 1f;
                                    parameters = new PlaybackParameters(speed);
                                    player.setPlaybackParameters(parameters);
                                    break;
                                case 2:
                                    speed = 1.25f;
                                    parameters = new PlaybackParameters(speed);
                                    player.setPlaybackParameters(parameters);
                                    break;
                                case 3:
                                    speed = 1.5f;
                                    parameters = new PlaybackParameters(speed);
                                    player.setPlaybackParameters(parameters);
                                    break;
                                case 4:
                                    speed = 2f;
                                    parameters = new PlaybackParameters(speed);
                                    player.setPlaybackParameters(parameters);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                    AlertDialog alert = alertDialog.create();
                    alert.show();
                }
                if (position == 9) {
                    dialogProperties.selection_mode = DialogConfigs.SINGLE_MODE;
                    dialogProperties.extensions = new String[]{".srt"};
                    dialogProperties.root = new File("/storage/emulated/0/");
                    filePickerDialog.setProperties(dialogProperties);
                    filePickerDialog.show();
                    filePickerDialog.setDialogSelectionListener(new DialogSelectionListener() {
                        @Override
                        public void onSelectedFilePaths(String[] files) {
                            for (String path : files) {
                                File file = new File(path);
                                uriSubtitle = Uri.parse(file.getAbsolutePath().toString());
                            }
                            playVideoSubtitle(uriSubtitle);
                        }
                    });
                }
            }
        });
    }

    private void initViews() {
        nextButton = findViewById(R.id.exo_next);
        preButton = findViewById(R.id.exo_prev);
        total_duration = findViewById(R.id.exo_duration);
        title = findViewById(R.id.video_title);
        lock = findViewById(R.id.lock);
        unlock = findViewById(R.id.unlock);
        scaling = findViewById(R.id.scaling);
        root = findViewById(R.id.root_layout);
        nightMode = findViewById(R.id.night_mode);
        videoList = findViewById(R.id.video_list);
        video_more = findViewById(R.id.video_more);
        recyclerViewIcons = findViewById(R.id.recyclerview_icon);
        eqControl = findViewById(R.id.eqFrame);
        vol_text = findViewById(R.id.vol_text);
        brt_text = findViewById(R.id.brt_text);
        volProgress = findViewById(R.id.vol_progress);
        brtProgress = findViewById(R.id.brt_progress);
        vol_progress_container = findViewById(R.id.vol_progress_container);
        brt_progress_container = findViewById(R.id.brt_progress_container);
        vol_text_container = findViewById(R.id.vol_text_container);
        brt_text_container = findViewById(R.id.brt_text_container);
        vol_icon = findViewById(R.id.vol_icon);
        brt_icon = findViewById(R.id.brt_icon);
        zoomLayout = findViewById(R.id.zoom_layout);
        zoomContainer = findViewById(R.id.zoom_container);
        zoom_perc = findViewById(R.id.zoom_percentage);
        double_tap_playpause = findViewById(R.id.double_tap_play_pause);
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleDetector());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(this) {
            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }
        };

        title.setText(videoTitle);

        nextButton.setOnClickListener(this);
        preButton.setOnClickListener(this);
        lock.setOnClickListener(this);
        unlock.setOnClickListener(this);
        videoList.setOnClickListener(this);
        video_more.setOnClickListener(this);
        scaling.setOnClickListener(firstListener);
        double milliseconds = Double.parseDouble(mVideoFiles.get(position).getDuration());
        total_duration.setText(Utility.timeConversion((long) milliseconds));

        dialogProperties = new DialogProperties();
        filePickerDialog = new FilePickerDialog(VideoPlayerActivity.this);
        filePickerDialog.setTitle(getString(R.string.select_a_subtitle_file));
        filePickerDialog.setPositiveBtnName(getString(R.string.ok));
        filePickerDialog.setNegativeBtnName(getString(R.string.cancel));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPicture = new PictureInPictureParams.Builder();
        }
    }

    private void playVideo() {
        String path = mVideoFiles.get(position).getPath();
        Uri uri = Uri.parse(path);
        player = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "app"));
        concatenatingMediaSource = new ConcatenatingMediaSource();
        for (int i = 0; i < mVideoFiles.size(); i++) {
            new File(String.valueOf(mVideoFiles.get(i)));
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                    .createMediaSource(Uri.parse(String.valueOf(uri)));
            concatenatingMediaSource.addMediaSource(mediaSource);
        }
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.setPlaybackParameters(parameters);
        player.prepare(concatenatingMediaSource);
        player.seekTo(position, C.TIME_UNSET);
        playError();
    }

    private void playVideoSubtitle(Uri subtitle) {
        long oldPosition = player.getCurrentPosition();
        player.stop();

        String path = mVideoFiles.get(position).getPath();
        Uri uri = Uri.parse(path);
        player = new SimpleExoPlayer.Builder(this).build();
        DefaultDataSourceFactory defaultDataSourceFactory = new DefaultDataSourceFactory(
                this, Util.getUserAgent(this, "app"));
        concatenatingMediaSource = new ConcatenatingMediaSource();
        for (int i = 0; i < mVideoFiles.size(); i++) {
            new File(String.valueOf(mVideoFiles.get(i)));
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                    .createMediaSource(Uri.parse(String.valueOf(uri)));
            Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP, Format.NO_VALUE, "app");
            MediaSource subtitleSource = new SingleSampleMediaSource.Factory(defaultDataSourceFactory).setTreatLoadErrorsAsEndOfStream(true)
                    .createMediaSource(Uri.parse(String.valueOf(subtitle)), textFormat, C.TIME_UNSET);
            MergingMediaSource mergingMediaSource = new MergingMediaSource(mediaSource, subtitleSource);
            concatenatingMediaSource.addMediaSource(mergingMediaSource);
        }
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        player.setPlaybackParameters(parameters);
        player.prepare(concatenatingMediaSource);
        player.seekTo(position, oldPosition);
        playError();
    }

    private void screenOrientation() {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            Bitmap bitmap;
            String path = mVideoFiles.get(position).getPath();
            Uri uri = Uri.parse(path);
            retriever.setDataSource(this, uri);
            bitmap = retriever.getFrameAtTime();

            int videoWidth = bitmap.getWidth();
            int videoHeight = bitmap.getHeight();
            if (videoWidth > videoHeight) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        } catch (Exception e) {
            Log.e("MediaMetadataRetriever", "screenOrientation: " + e.getMessage());
        }
    }

    private void playError() {
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toasty.error(VideoPlayerActivity.this, getString(R.string.video_playing_error), Toast.LENGTH_SHORT).show();
                Player.Listener.super.onPlayerError(error);
            }
        });
        player.setPlayWhenReady(true);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.eqFrame);
        if (eqControl.getVisibility() == View.GONE) {
            super.onBackPressed();
        } else {
            if (fragment.isVisible() && eqControl.getVisibility() == View.VISIBLE) {
                eqControl.setVisibility(View.GONE);
            } else {
                if (player != null) {
                    player.release();
                }
                super.onBackPressed();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onPause() {
        super.onPause();
        player.setPlayWhenReady(false);
        player.getPlaybackState();
        if (isInPictureInPictureMode()) {
            player.setPlayWhenReady(true);
        } else {
            player.setPlayWhenReady(false);
            player.getPlaybackState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        player.setPlayWhenReady(true);
        player.getPlaybackState();
    }

    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public void hideBottomBar() {
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.exo_next) {
            try {
                player.stop();
                position++;
                title.setText(mVideoFiles.get(position).getDisplayName());
                playVideo();
            } catch (Exception e) {
                Toasty.info(this, getString(R.string.no_next_video), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (id == R.id.exo_prev) {
            try {
                player.stop();
                position--;
                title.setText(mVideoFiles.get(position).getDisplayName());
                playVideo();
            } catch (Exception e) {
                Toasty.info(this, getString(R.string.no_previous_video), Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (id == R.id.lock) {
            controlsMode = ControlsMode.FULLSCREEN;
            root.setVisibility(View.VISIBLE);
            lock.setVisibility(View.INVISIBLE);
            Toasty.info(this, getString(R.string.unlocked), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.unlock) {
            controlsMode = ControlsMode.LOCK;
            root.setVisibility(View.INVISIBLE);
            lock.setVisibility(View.VISIBLE);
            Toasty.info(this, getString(R.string.locked), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.video_list) {
            PlaylistDialog playlistDialog = new PlaylistDialog(mVideoFiles, videoFileAdapter);
            playlistDialog.show(getSupportFragmentManager(), playlistDialog.getTag());
        } else if (id == R.id.video_more) {
            PopupMenu popupMenu = new PopupMenu(VideoPlayerActivity.this, video_more);
            popupMenu.inflate(R.menu.player_menu);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    int id = menuItem.getItemId();
                    if (id == R.id.next) {
                        nextButton.performClick();
                    } else if (id == R.id.send) {
                        Uri uri = Uri.parse(mVideoFiles.get(position).getPath());
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_video_via)));
                    } else if (id == R.id.properties) {
                        double milliSeconds = Double.parseDouble(mVideoFiles.get(position).getDuration());
                        String one = "File: " + mVideoFiles.get(position).getDisplayName();
                        String path = mVideoFiles.get(position).getPath();
                        int indexOfPath = path.lastIndexOf("/");
                        String two = "Path: " + path.substring(0, indexOfPath);
                        String three = "Size: " + android.text.format.Formatter
                                .formatFileSize(VideoPlayerActivity.this, Long.parseLong(mVideoFiles.get(position).getSize()));
                        String four = "Length: " + Utility.timeConversion((long) milliSeconds);
                        String nameWithFormat = mVideoFiles.get(position).getDisplayName();
                        int index = nameWithFormat.lastIndexOf(".");
                        String format = nameWithFormat.substring(index + 1);
                        String five = "Format: " + format;

                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(mVideoFiles.get(position).getPath());
                        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                        String six = "Resolution: " + width + "X" + height;

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                                .setTitle(R.string.properties)
                                .setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + four + "\n\n" + five + "\n\n" + six)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                        alertDialog.show();
                    } else if (id == R.id.delete) {
                        AlertDialog.Builder alertDialogDelete = new AlertDialog.Builder(VideoPlayerActivity.this);
                        alertDialogDelete.setTitle(R.string.delete);
                        alertDialogDelete.setMessage(R.string.are_you_sure_you_want_to_delete);
                        alertDialogDelete.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Uri contentUri = ContentUris
                                        .withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                Long.parseLong(mVideoFiles.get(position).getId()));
                                File file = new File(mVideoFiles.get(position).getPath());
                                boolean delete = file.delete();
                                if (delete) {
                                    getContentResolver().delete(contentUri, null, null);
                                    mVideoFiles.remove(position);
                                    nextButton.performClick();
                                    Toasty.info(VideoPlayerActivity.this, getString(R.string.video_deleted), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toasty.error(VideoPlayerActivity.this, getString(R.string.can_t_deleted), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();

                    } else if (id == R.id.subtitle) {
                        dialogProperties.selection_mode = DialogConfigs.SINGLE_MODE;
                        dialogProperties.extensions = new String[]{".srt"};
                        dialogProperties.root = new File("/storage/emulated/0/");
                        filePickerDialog.setProperties(dialogProperties);
                        filePickerDialog.show();
                        filePickerDialog.setDialogSelectionListener(new DialogSelectionListener() {
                            @Override
                            public void onSelectedFilePaths(String[] files) {
                                for (String path : files) {
                                    File file = new File(path);
                                    uriSubtitle = Uri.parse(file.getAbsolutePath().toString());
                                }
                                playVideoSubtitle(uriSubtitle);
                            }
                        });
                    }
                    return true;
                }
            });
            popupMenu.show();
        }
    }

    View.OnClickListener firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.fullscreen);

            Toasty.info(VideoPlayerActivity.this, getString(R.string.full_screen), Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(secondListener);
        }
    };
    View.OnClickListener secondListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.zoom);

            Toasty.info(VideoPlayerActivity.this, R.string.zoom, Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(thirdListener);
        }
    };
    View.OnClickListener thirdListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            player.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            scaling.setImageResource(R.drawable.fit);

            Toasty.info(VideoPlayerActivity.this, getString(R.string.fit), Toast.LENGTH_SHORT).show();
            scaling.setOnClickListener(firstListener);
        }
    };

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        isCrossChecked = isInPictureInPictureMode;
        if (isInPictureInPictureMode) {
            playerView.hideController();
        } else {
            playerView.showController();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isCrossChecked) {
            player.release();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 111) {
            boolean value;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                value = android.provider.Settings.System.canWrite(getApplicationContext());
                if (value) {
                    success = true;
                } else {
                    Toasty.error(this, R.string.no_generated, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private class ScaleDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            scale_factor *= detector.getScaleFactor();
            scale_factor = Math.max(0.5f, Math.min(scale_factor, 6.0f));

            zoomLayout.setScaleX(scale_factor);
            zoomLayout.setScaleY(scale_factor);
            int percentage = (int) (scale_factor * 100);
            zoom_perc.setText("" + percentage + "%");
            zoomContainer.setVisibility(View.VISIBLE);

            brt_text_container.setVisibility(View.GONE);
            vol_text_container.setVisibility(View.GONE);
            brt_progress_container.setVisibility(View.GONE);
            vol_progress_container.setVisibility(View.GONE);

            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            zoomContainer.setVisibility(View.GONE);
            super.onScaleEnd(detector);
        }
    }
}