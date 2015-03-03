package de.timomeh.podcasts.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import de.timomeh.podcasts.R;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.ui.activities.MainActivity;

/**
 * Created by Timo Maemecke (@timomeh) on 25/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class PlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = PlayerService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;
    public static final String EXTRA_EPISODE_UUID = "episodeUuid";
    public static final String ACTION_PLAY = "de.timomeh.podcasts.action.PLAY";
    public static final String ACTION_RESUME_PLAYER = "de.timomeh.podcasts.action.RESUME_PLAYER";

    private MediaPlayer mPlayer;
    private Episode mEpisode;
    private int mBufferPercentage = 0;
    private boolean mIsStreaming = false;
    private WifiManager.WifiLock mWifiLock;
    private final IBinder mPlayerBinder = new PlayerBinder();

    // START: Service Lifecycle
    // ========================
    @Override
    public void onCreate() {
        // TODO: Audio manager requestAudioFocus
        super.onCreate();
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "PodcastWifiLock");
    }

    // startService Lifecycle
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals(ACTION_PLAY)) {
            mPlayer = new MediaPlayer();
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    // bindService Lifecycle
    @Override
    public IBinder onBind(Intent intent) {
        return mPlayerBinder;
    }

    // bindService Lifecycle
    @Override
    public boolean onUnbind(Intent intent) {
        mPlayer.stop();
        mPlayer.release();
        stopForeground(true);
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }

    // END: Service Lifecycle
    // ======================

    // START: MediaPlayer Implementations
    // ==================================
    @Override
    public void onCompletion(MediaPlayer mp) {
        stopForeground(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO: onError enhance
        mp.release();
        mp.reset();
        stopForeground(true);
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        showNotification();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        mBufferPercentage = percent;
    }
    // END: MediaPlayer Implementations
    // ================================

    // START: AudioManager Implementations
    // ===================================
    @Override
    public void onAudioFocusChange(int focusChange) {
        // TODO: what to do when focus changes?
    }
    // END: AudioManager Implementations
    // =================================

    public void setEpisode(Episode episode) {
        mEpisode = episode;
    }

    public Episode getEpisode() {
        return mEpisode;
    }


    public void playEpisode() {
        mPlayer.reset();

        Uri episodeUri = null;
        if (!mEpisode.getFileuri().equals("")) {
            mIsStreaming = false;
            episodeUri = Uri.parse(mEpisode.getFileuri());
            Log.d("PlayerService", "Playing file (local).");
        } else if (!mEpisode.getFileurl().equals("")) {
            mIsStreaming = true;
            episodeUri = Uri.parse(mEpisode.getFileurl());
            Log.d("PlayerService", "Straming file (remote).");
        }

        if (mIsStreaming && mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }

        if (episodeUri != null) {
            try {
                mPlayer.setDataSource(getApplicationContext(), episodeUri);
            } catch (IOException e) {
                Log.e(TAG, "Error setting data source:", e);
            }
            mPlayer.prepareAsync();
        } else {
            Toast.makeText(getApplicationContext(), "Can't play episode.", Toast.LENGTH_LONG).show();
        }
    }

    public int getDuration() {
        return mPlayer.getDuration();
    }

    public int getPosition() {
        return mPlayer.getCurrentPosition();
    }

    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    public boolean isPlaying() {
        try {
            return mPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void pause() {
        mPlayer.pause();
        stopForeground(true);
        if (mIsStreaming && mWifiLock != null) {
            try {
                mWifiLock.release();
            } catch (Exception e) {
                // Intentionally left blank
                // Probably WifiLock is already released
            }
        }
    }

    public void start() {
        mPlayer.start();
        showNotification();
    }

    public void stop() {
        mPlayer.stop();
        stopForeground(true);
    }

    public void seek(int msec) {
        mPlayer.seekTo(msec);
    }

    private void showNotification() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        Notification notification = new Notification();
        notification.tickerText = "Playing " + mEpisode.getTitle();
        notification.icon = android.R.drawable.stat_sys_headset;
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(getApplicationContext(), mEpisode.getTitle(), mEpisode.getPodcast().getTitle(), pi);
        startForeground(NOTIFICATION_ID, notification);
    }

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }
}
