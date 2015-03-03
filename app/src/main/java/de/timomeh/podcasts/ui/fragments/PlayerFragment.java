package de.timomeh.podcasts.ui.fragments;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.controller.PlayerController;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.services.PlayerService;
import de.timomeh.podcasts.utils.BlurTransformation;
import de.timomeh.podcasts.utils.StringHelper;
import de.timomeh.podcasts.utils.StyleHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayerFragment extends Fragment implements MediaController.MediaPlayerControl {

    public PlayerFragment() { }

    public interface OnPlayerInteraction {
        void onPause(Episode episode);
        void onPlay(Episode episode);
    }

    private PlayerController mPlayerController;
    public PlayerService mPlayerService;
    private Intent mPlayerIntent;
    private boolean mPlayerBound = false;
    private Episode mEpisode;
    private Handler mHandler;
    private int mColor = Color.parseColor("#121212");
    private OnPlayerInteraction mOnPlayerInteraction;

    private boolean mPaused = false;
    private boolean mPlayerPaused = false;

    @InjectView(R.id.player_bar_image) ImageView mPlayerBarImage;
    @InjectView(R.id.player_bar_track) TextView mPlayerTrackName;
    @InjectView(R.id.player_bar_progress) ProgressBar mPlayerBarProgress;
    @InjectView(R.id.player_bar_action) ImageView mPlayerBarAction;
    @InjectView(R.id.player_background) ImageView mPlayerBackground;
    @InjectView(R.id.player_image) ImageView mPlayerImage;
    @InjectView(R.id.player_seek) SeekBar mPlayerSeek;
    @InjectView(R.id.player_epi_title) TextView mPlayerEpisodeTitle;
    @InjectView(R.id.player_pod_title) TextView mPlayerPodcastTitle;
    @InjectView(R.id.player_dur_current) TextView mPlayerDurationCurrent;
    @InjectView(R.id.player_dur_total) TextView mPlayerDurationTotal;
    @InjectView(R.id.player_play_pause) ImageView mPlayerPlayPause;

    // START: Fragment Lifecycle
    // =========================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPlayerIntent == null) {
            mPlayerIntent = new Intent(getActivity(), PlayerService.class);
            mPlayerIntent.setAction(PlayerService.ACTION_PLAY);
            getActivity().getApplicationContext().bindService(mPlayerIntent, mPlayerConnection, Context.BIND_AUTO_CREATE);
            getActivity().getApplicationContext().startService(mPlayerIntent);
        }

        mPlayerController = new PlayerController(getActivity());
        mPlayerController.setMediaPlayer(this);
        mPlayerController.setEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        mPlayerController.hide();
        stopCheckingTask();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(mPlayerConnection);
    }
    // END: Fragment Lifecycle
    // =======================

    // START: MediaPlayerControl Implementations
    // =========================================
    @Override
    public void start() {
        mPlayerPaused = false;
        mPlayerService.start();
        mOnPlayerInteraction.onPlay(mEpisode);
        setControls(true);
        startCheckingTask();
    }

    @Override
    public void pause() {
        mPlayerPaused = true;
        mPlayerService.pause();
        mOnPlayerInteraction.onPause(mEpisode);
        setControls(false);
        stopCheckingTask();
    }

    @Override
    public int getDuration() {
        if (mPlayerBound && mPlayerService != null && mPlayerService.isPlaying()) {
            return mPlayerService.getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (mPlayerBound && mPlayerService != null && mPlayerService.isPlaying()) {
            return mPlayerService.getPosition();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        mPlayerService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        return mPlayerService.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mPlayerBound && mPlayerService != null && mPlayerService.isPlaying()) {
            return mPlayerService.getBufferPercentage();
        } else {
            return 0;
        }
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
    // END: MediaPlayerControl Implementations
    // =======================================

    public void setOnPlayerInteractionListener(OnPlayerInteraction listener) {
        mOnPlayerInteraction = listener;
    }

    public void setEpisode(Episode episode) {
        mEpisode = episode;
        mPlayerService.setEpisode(mEpisode);
        mPlayerService.playEpisode();
        startCheckingTask();
        if (mPlayerPaused) {
            mPlayerPaused = false;
        }
        mPlayerController.show();
        setControls(true);
        setViews();
    }

    public Episode getEpisode() {
        return mEpisode;
    }

    public void setViews() {
        String imageurl = mEpisode.getImageurl();
        if (imageurl.equals("")) {
            imageurl = mEpisode.getPodcast().getImageurl();
        }

        mPlayerEpisodeTitle.setText(mEpisode.getTitle());
        mPlayerPodcastTitle.setText(mEpisode.getPodcast().getTitle());
        mPlayerDurationTotal.setText(StringHelper.convertDuration(mEpisode.getDuration()));
        mPlayerDurationCurrent.setText(StringHelper.convertDuration(0));

        int playerBarImageDimen = (int) StyleHelper.convertDpToPixel(56, getActivity());
        mPlayerTrackName.setText(mEpisode.getTitle());
        Picasso.with(getActivity())
                .load(imageurl)
                .resize(playerBarImageDimen, playerBarImageDimen)
                .centerCrop()
                .into(mPlayerBarImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap podcastImage = ((BitmapDrawable) mPlayerBarImage.getDrawable()).getBitmap();
                        Palette.generateAsync(podcastImage, new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                int color = palette.getDarkVibrantColor(Color.parseColor(getString(R.color.dark_grey)));
                                mColor = color;
                            }
                        });
                    }

                    @Override
                    public void onError() {

                    }
                });

        int playerImageDimen = (int) StyleHelper.convertDpToPixel(256, getActivity());
        Picasso.with(getActivity())
                .load(imageurl)
                .resize(playerImageDimen, playerImageDimen)
                .centerCrop()
                .into(mPlayerImage);

        Picasso.with(getActivity())
                .load(imageurl)
                .resize(playerImageDimen, playerImageDimen)
                .centerCrop()
                .transform(new BlurTransformation(50)).into(mPlayerBackground);

        mPlayerBarAction.setOnClickListener(mPlayPause);
        mPlayerPlayPause.setOnClickListener(mPlayPause);

        mPlayerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int msec = (int) ((double) getDuration() * ((double) progress/(double) seekBar.getMax()));
                    seekTo(msec);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public int getStatusBarColor() {
        return mColor;
    }

    public void setCurrentPosition(final int seconds) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPlayerDurationCurrent.setText(StringHelper.convertDuration(Long.parseLong(seconds + "")) + "");
            }
        });

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            mHandler.postDelayed(mStatusChecker, 200);
        }
    };

    void updateStatus() {
        int duration = getDuration();
        int current = getCurrentPosition();
        int progress = (int) (((double) current / (double) duration) * 10000);
        mPlayerBarProgress.setProgress(progress);
        mPlayerBarProgress.setSecondaryProgress(getBufferPercentage() * 100);
        mPlayerSeek.setProgress(progress);
        mPlayerSeek.setSecondaryProgress(getBufferPercentage() * 100);
        mPlayerDurationCurrent.setText(StringHelper.convertDuration(getCurrentPosition() / 1000));
        mPlayerDurationTotal.setText(StringHelper.convertDuration(getDuration() / 1000));
    }

    void startCheckingTask() {
        mStatusChecker.run();
    }

    void stopCheckingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private void setControls(boolean play) {
        if (play) {
            mPlayerBarAction.setImageResource(R.drawable.ic_pause_white_24dp);
            mPlayerPlayPause.setImageResource(R.drawable.ic_pause_white_24dp);
        } else {
            mPlayerBarAction.setImageResource(R.drawable.ic_play_white_24dp);
            mPlayerPlayPause.setImageResource(R.drawable.ic_play_white_24dp);
        }
    }

    private View.OnClickListener mPlayPause = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mPlayerPaused) {
                pause();
            } else {
                start();
            }
        }
    };

    private ServiceConnection mPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mPlayerService = binder.getService();
            mPlayerService.setEpisode(mEpisode);
            setControls(true);
            mPlayerBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerBound = false;
        }
    };
}
