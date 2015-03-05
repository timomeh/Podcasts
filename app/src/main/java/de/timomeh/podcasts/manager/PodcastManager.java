package de.timomeh.podcasts.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.Headers;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.models.Podcast;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

/**
 * Created by Timo Maemecke (@timomeh) on 13/02/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class PodcastManager {

    public interface OnCheckListener {
        void onStatusChecked(boolean upToDate);
        void onFailure();
    }

    public interface OnUpdateListener {
        void onUpdated(boolean changed);
        void onFailure();
    }

    public interface OnSaveListener {
        void onSave(Podcast podcast);
        void onFailure();
    }

    private Podcast mPodcast;
    private Podcast mFetchedPodcast;
    private List<Episode> mFetchedEpisodes;
    private Context mContext;
    private int mSkip = 0;
    private int mLimit = -1;
    private OnCheckListener mOnCheckListener;
    private OnUpdateListener mOnUpdateListener;
    private OnSaveListener mOnSaveListener;

    public PodcastManager(Context c, Podcast podcast) {
        mContext = c;
        mPodcast = new Podcast();
        mPodcast.setUuid(podcast.getUuid());
        mPodcast.setFeedurl(podcast.getFeedurl());
        mPodcast.setEtag(podcast.getEtag());
        mPodcast.setLastmodified(podcast.getLastmodified());
    }

    public void setOnCheckListener(OnCheckListener listener) {
        mOnCheckListener = listener;
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        mOnUpdateListener = listener;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        mOnSaveListener = listener;
    }

    public PodcastManager setSkip(int skip) {
        mSkip = skip;
        return this;
    }

    public PodcastManager setLimit(int limit) {
        mLimit = limit;
        return this;
    }

    private void request(boolean onlyHead, SyndicationFeed.OnResponseListener responseListener, SyndicationFeed.OnBuildListener buildListener) {
        URL feedUrl = null;
        try {
            feedUrl = new URL(mPodcast.getFeedurl());
            Headers.Builder headersBuilder = new Headers.Builder();
            if (mPodcast.getEtag() != null && !mPodcast.getEtag().equals("")) {
                headersBuilder.add("If-None-Match", mPodcast.getEtag());
            } else if (mPodcast.getLastmodified() != null && !mPodcast.getLastmodified().equals("")) {
                headersBuilder.add("If-Modified-Since", mPodcast.getLastmodified());
            }
            Headers headers = headersBuilder.build();
            SyndicationFeed syndicationFeed = new SyndicationFeed(feedUrl);
            syndicationFeed.setOnResponseListener(responseListener);
            if (buildListener != null) syndicationFeed.setOnBuildListener(buildListener);
            if (onlyHead)
                syndicationFeed.head(headers);
            else
                syndicationFeed.get(headers);
        } catch (MalformedURLException e) {
            if (onlyHead)
                mOnCheckListener.onFailure();
            else
                mOnUpdateListener.onFailure();
        }
    }

    public void check() {
        SyndicationFeed.OnResponseListener listener = new SyndicationFeed.OnResponseListener() {
            @Override
            public void onSuccess(Headers headers) {
                mOnCheckListener.onStatusChecked(false);
            }

            @Override
            public void onNotModified() {
                mOnCheckListener.onStatusChecked(true);
            }

            @Override
            public void onFailure() {
                mOnCheckListener.onFailure();
            }
        };
        request(true, listener, null);
    }

    private void handle(final boolean update) {
        final String[] etag = {""};
        final String[] lastModified = {""};
        SyndicationFeed.OnResponseListener responseListener = new SyndicationFeed.OnResponseListener() {
            @Override
            public void onSuccess(Headers headers) {
                if (headers.get("etag") != null) etag[0] = headers.get("etag");
                if (headers.get("last-modified") != null) lastModified[0] = headers.get("last-modified");
            }

            @Override
            public void onNotModified() {
                mOnUpdateListener.onUpdated(false);
            }

            @Override
            public void onFailure() {
                if (update) mOnUpdateListener.onFailure();
                else mOnSaveListener.onFailure();
            }
        };
        SyndicationFeed.OnBuildListener buildListener = new SyndicationFeed.OnBuildListener() {
            @Override
            public void onBuild(Podcast podcast, List<Episode> episodes) {
                mFetchedPodcast = podcast;
                mFetchedEpisodes = episodes;

                mFetchedPodcast.setEtag(etag[0]);
                mFetchedPodcast.setLastmodified(lastModified[0]);

                // onBuild returns podcast = null if an error occured.
                // Since we've already set some Variables in mFetchedPodcast, we just check if there is a feed url.
                // If there is none, there is no Podcast either.

                if (mFetchedPodcast.getFeedurl() == null || mFetchedPodcast.getFeedurl().equals("")) {
                    if (update) {
                        Realm realm = Realm.getInstance(mContext);

                        // Find Podcast to Update in Database and set it as error'ed
                        Podcast uPod = realm.where(Podcast.class).equalTo("uuid", mPodcast.getUuid()).findFirst();
                        realm.beginTransaction();
                        uPod.setError(true);
                        realm.commitTransaction();

                        showToast("Can't update " + uPod.getTitle() + ", Feed has invalid format.");
                    } else {
                        showToast("Can't subscribe to Podcast, Feed has invalid format.");
                    }
                }
                else if (update)
                    updatePodcast();
                else
                    savePodcast();
            }
        };
        request(false, responseListener, buildListener);
    }

    public void save() {
        handle(false);
    }

    public void update() {
        handle(true);
    }



    /**
     * Saves the parsed response in the database.
     */
    private void savePodcast() {
        Realm realm = Realm.getInstance(mContext);
        realm.beginTransaction();

        // Copy Podcast Object to Realm
        Podcast realmPodcast = realm.copyToRealm(mFetchedPodcast);
        realmPodcast.setUuid(UUID.randomUUID().toString());

        for (Episode episode : mFetchedEpisodes) {
            // Set all episodes to seen when subscribing to a new podcast
            episode.setSeen(true);

            // Set UUID to episode
            episode.setUuid(UUID.randomUUID().toString());

            // Copy Episode Object to Realm
            Episode realmEpisode = realm.copyToRealm(episode);

            // Set the relation to the podcast
            realmEpisode.setPodcast(realmPodcast);

            // Set the relation from the podcast
            realmPodcast.getEpisodes().add(realmEpisode);
        }

        // Yay!
        realm.commitTransaction();
        mOnSaveListener.onSave(mFetchedPodcast);
        realm.close();
    }

    /**
     * Updates
     */
    private void updatePodcast() {
        Realm realm = Realm.getInstance(mContext);
        Podcast realmPodcast = realm.where(Podcast.class).equalTo("uuid", mPodcast.getUuid()).findFirst();
        RealmList<Episode> oldEpisodes = realmPodcast.getEpisodes();

        realm.beginTransaction();

        // Update Podcast
        realmPodcast.setFeedurl(mFetchedPodcast.getFeedurl());
        realmPodcast.setTitle(mFetchedPodcast.getTitle());
        realmPodcast.setAuthor(mFetchedPodcast.getAuthor());
        realmPodcast.setImageurl(mFetchedPodcast.getImageurl());
        realmPodcast.setWeburl(mFetchedPodcast.getWeburl());
        realmPodcast.setSubtitle(mFetchedPodcast.getSubtitle());
        realmPodcast.setSummary(mFetchedPodcast.getSummary());
        realmPodcast.setExplicit(mFetchedPodcast.isExplicit());
        realmPodcast.setEtag(mFetchedPodcast.getEtag());
        realmPodcast.setLastmodified(mFetchedPodcast.getLastmodified());
        realmPodcast.setUptodate(true);

        // Update each Episode
        for (int i = 0; i < mFetchedEpisodes.size(); i++) {
            Episode fresh = mFetchedEpisodes.get(i);
            fresh.setUpdated(false);

            // Check if Episode is already in database. If it is, update it. Just in case.
            int k = 0;
            for (Episode old : oldEpisodes) {
                if (fresh.getGuid().equals(old.getGuid())) {
                    old.setTitle(fresh.getTitle());
                    old.setImageurl(fresh.getImageurl());
                    old.setFileurl(fresh.getFileurl());
                    old.setFileuri(fresh.getFileuri());
                    old.setFilesize(fresh.getFilesize());
                    old.setFiletype(fresh.getFiletype());
                    old.setDuration(fresh.getDuration());
                    old.setSummary(fresh.getSummary());
                    old.setExplicit(fresh.isExplicit());
                    old.setUpdated(true);
                    fresh.setUpdated(true);
                }
                k++;
            }

            // If Episode is not updated, it's new! Insert it.
            if (!fresh.isUpdated()) {
                fresh.setSeen(false);
                fresh.setUuid(UUID.randomUUID().toString());
                Episode freshRealmEpisode = realm.copyToRealm(fresh);
                freshRealmEpisode.setPodcast(realmPodcast);
            }
        }

        // Check if "old" Episode was updated. If not, it's not anymore in the feed. Delete it.
        for (Episode old : oldEpisodes) {
            if (!old.isUpdated()) {
                old.removeFromRealm();
            }
            old.setUpdated(false);
        }

        // Yay!
        realm.commitTransaction();
        mOnUpdateListener.onUpdated(true);
        realm.close();
    }

    public void delete() {
        Realm realm = Realm.getInstance(mContext);
        realm.beginTransaction();

        Podcast podcast = realm.where(Podcast.class).equalTo("uuid", mPodcast.getUuid()).findFirst();
        List<Episode> episodes = podcast.getEpisodes();

        // Remove each Episode
        for (int i = episodes.size(); i > 0; i--) {
            // If file is downloaded, delete it first
            String fileuri = episodes.get(i-1).getFileuri();
            if (fileuri != null) {
                File file = new File(fileuri);
                file.delete();
            }
            episodes.get(i-1).removeFromRealm();
        }

        // Remove Podcast
        podcast.removeFromRealm();

        // Yay!
        realm.commitTransaction();
        realm.close();
    }

    private void showToast(final String message) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
            }
        });
    }


}
