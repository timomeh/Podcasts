package de.timomeh.podcasts.ui.fragments;

import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.pojo.Duration;
import de.timomeh.podcasts.receiver.DownloadReceiver;
import de.timomeh.podcasts.services.DownloadService;
import de.timomeh.podcasts.ui.activities.MainActivity;
import de.timomeh.podcasts.utils.StringHelper;
import de.timomeh.podcasts.utils.StyleHelper;
import io.realm.Realm;


public class EpisodeDetailsFragment extends Fragment {

    public EpisodeDetailsFragment() {  }

    private Episode mEpisode;
    private Realm mRealm;
    private PlayerFragment mPlayerFragment;
    private boolean mIsInPlayer;

    private Toolbar mToolbar;

    @InjectView(R.id.epi_image) ImageView mEpisodeImage;
    @InjectView(R.id.epi_title) TextView mEpisodeTitle;
    @InjectView(R.id.epi_duration) TextView mEpisodeDuration;
    @InjectView(R.id.epi_download) TextView mEpisodeDownload;
    @InjectView(R.id.epi_row_download) TableRow mEpisodeDownloadRow;
    @InjectView(R.id.epi_web) TextView mEpisodeWeb;
    @InjectView(R.id.epi_summary) TextView mEpisodeSummary;
    @InjectView(R.id.epi_action) FloatingActionButton mEpisodeAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getInstance(getActivity());
        mPlayerFragment = (PlayerFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_player);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_episode_details, container, false);
        ButterKnife.inject(this, view);
        mToolbar = (Toolbar) getActivity().findViewById(R.id.main_toolbar);

        Bundle args = getArguments();
        receive(args);

        Duration duration = new Duration(mEpisode.getDuration());

        setImage();
        checkRunningPlayer();
        mToolbar.setTitle(mEpisode.getPodcast().getTitle());
        mEpisodeTitle.setText(mEpisode.getTitle());
        mEpisodeDuration.setText(duration.getTwoDigitMinutes() + " Min. " + duration.getTwoDigitSeconds() + " Sec.");
        if (!mEpisode.getFileuri().equals("")) {
            mEpisodeDownload.setText("Downloaded");
            mEpisodeDownloadRow.setOnClickListener(mOnDeleteFileClickListener);
        } else {
            mEpisodeDownload.setText("Download (" + StringHelper.humanByteCount(mEpisode.getFilesize(), true) + ")");
            mEpisodeDownloadRow.setOnClickListener(mOnDownloadClickListener);
        }
        mEpisodeWeb.setText(mEpisode.getWeburl());
        mEpisodeSummary.setText(mEpisode.getSummary());

        if (mIsInPlayer) {
            mEpisodeAction.setIcon(R.drawable.ic_pause_white_24dp);
            mEpisodeAction.setOnClickListener(mOnCurrentEpisodeAction);
        } else {
            mEpisodeAction.setOnClickListener(mOnNewEpisodeAction);
        }

        mPlayerFragment.setOnPlayerInteractionListener(new PlayerFragment.OnPlayerInteraction() {
            @Override
            public void onPause(Episode episode) {
                if (mIsInPlayer) {
                    mEpisodeAction.setIcon(R.drawable.ic_play_white_24dp);
                }
            }

            @Override
            public void onPlay(Episode episode) {
                if (mIsInPlayer) {
                    mEpisodeAction.setIcon(R.drawable.ic_pause_white_24dp);
                }
            }
        });

        return view;
    }

    private void receive(Bundle args) {
        String episodeUuid = args.getString(MainActivity.ARGS_EPISODE_UUID);

        mEpisode = mRealm.where(Episode.class).equalTo("uuid", episodeUuid).findFirst();
        if (!mEpisode.isSeen()) {
            mRealm.beginTransaction();
            mEpisode.setSeen(true);
            mRealm.commitTransaction();
        }
    }

    private void setImage() {
        String imageUrl = mEpisode.getImageurl();
        if (imageUrl.equals("")) {
            imageUrl = mEpisode.getPodcast().getImageurl();
        }

        Picasso.with(getActivity())
                .load(imageUrl)
                .placeholder(R.drawable.podcast_placeholder)
                .into(mEpisodeImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap podcastImage = ((BitmapDrawable) mEpisodeImage.getDrawable()).getBitmap();
                        Palette.generateAsync(podcastImage, new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                int color = palette.getDarkVibrantColor(Color.parseColor(getString(R.color.dark_grey)));
                                int darkColor = StyleHelper.darkenColor(color, 0.85f);

                                ((MainActivity) getActivity()).setStatusBarTint(color);
                                mToolbar.setBackgroundColor(color);
                                mEpisodeAction.setColorNormal(color);
                                mEpisodeAction.setColorPressed(darkColor);
                            }
                        });
                    }

                    @Override
                    public void onError() {

                    }
                });

    }

    private void checkRunningPlayer() {
        PlayerFragment playerFragment = (PlayerFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_player);
        if (playerFragment.mPlayerService != null &&
                playerFragment.mPlayerService.getEpisode() != null &&
                playerFragment.mPlayerService.getEpisode().getGuid().equals(mEpisode.getGuid())) {
            mIsInPlayer = true;
        }
    }

    private View.OnClickListener mOnNewEpisodeAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlayerFragment.setEpisode(mEpisode);
            mIsInPlayer = true;
            mEpisodeAction.setIcon(R.drawable.ic_pause_white_24dp);
            mEpisodeAction.setOnClickListener(mOnCurrentEpisodeAction);
            ((MainActivity) getActivity()).showPanel();
        }
    };

    private View.OnClickListener mOnCurrentEpisodeAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPlayerFragment.isPlaying()) {
                mEpisodeAction.setIcon(R.drawable.ic_play_white_24dp);
                mPlayerFragment.pause();
            } else {
                mEpisodeAction.setIcon(R.drawable.ic_pause_white_24dp);
                mPlayerFragment.start();
            }
        }
    };

    private View.OnClickListener mOnDownloadClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int notifyId = 2;
            final int[] publicProgress = {0};

            final NotificationManager notifyManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
            final NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(getActivity());
            notifyBuilder.setContentTitle("Download " + mEpisode.getTitle());
            notifyBuilder.setContentText("Download in progress");
            notifyBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
            notifyBuilder.setOngoing(true);
            notifyBuilder.setAutoCancel(true);

            final Timer notifyTimer = new Timer();
            notifyTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    notifyBuilder.setProgress(100, publicProgress[0], false);
                    Notification notify = notifyBuilder.build();
                    notify.flags |= Notification.FLAG_NO_CLEAR;
                    notifyManager.notify(notifyId, notify);
                }
            }, 0, 1000);

            DownloadReceiver receiver = new DownloadReceiver(new Handler());
            receiver.setOnProgressListener(new DownloadReceiver.OnProgressListener() {
                @Override
                public void onProgress(int progress) {
                    mEpisodeDownload.setText("Downloading… (" + progress + "%)");
                    publicProgress[0] = progress;
                }

                @Override
                public void onFinished(String destination) {
                    notifyTimer.cancel();
                    notifyBuilder.setContentText("Download complete")
                            .setProgress(0,0,false)
                            .setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done);
                    notifyManager.notify(notifyId, notifyBuilder.build());

                    mRealm.beginTransaction();
                    mEpisode.setFileuri(destination);
                    mRealm.commitTransaction();
                    mEpisodeDownload.setText("Downloaded");
                }
            });
            Intent intent = new Intent(getActivity(), DownloadService.class);
            intent.putExtra("url", mEpisode.getFileurl());
            intent.putExtra("receiver", receiver);
            getActivity().startService(intent);
        }
    };

    private View.OnClickListener mOnDeleteFileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String fileuri = mEpisode.getFileuri();
            if (fileuri != null) {
                File file = new File(fileuri);
                file.delete();
            }
            mRealm.beginTransaction();
            mEpisode.setFileuri("");
            mRealm.commitTransaction();
            mEpisodeDownload.setText("Download (" + StringHelper.humanByteCount(mEpisode.getFilesize(), true) + ")");
        }
    };
}
