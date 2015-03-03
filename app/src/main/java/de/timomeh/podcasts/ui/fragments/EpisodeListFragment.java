package de.timomeh.podcasts.ui.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.adapter.EpisodesAdapter;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.models.Podcast;
import de.timomeh.podcasts.ui.activities.MainActivity;
import de.timomeh.podcasts.utils.StyleHelper;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Timo Maemecke (@timomeh) on 25/01/15.
 *
 * Show the Detail View for a Podcast (Informations and Episodes)
 */
public class EpisodeListFragment extends Fragment implements EpisodesAdapter.EpisodeAdapterListener {

    public EpisodeListFragment() { }

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Toolbar mToolbar;
    private Podcast mPodcast;
    private List<Episode> mEpisodes;
    private int mColor;
    private boolean mIsScrolledDown;
    private int mScrollPosition = 0;
    private Realm mRealm;

    @InjectView(R.id.pod_details_recycler) RecyclerView mRecyclerView;
    @InjectView(R.id.pod_details_shadow) View mShadow;

    private int lastTopValueAssigned = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getInstance(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_episode_list, container, false);
        ButterKnife.inject(this, view);

        mToolbar = (Toolbar) getActivity().findViewById(R.id.main_toolbar);

        mToolbar.setBackgroundColor(Color.parseColor("#00000000"));
        mToolbar.setTitle("");

        Bundle args = getArguments();
        receive(args);

        mRecyclerView.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);


        mAdapter = new EpisodesAdapter(getActivity(), mEpisodes, EpisodeListFragment.this);
        mRecyclerView.setAdapter(mAdapter);

        getColorFromImage();

        // TODO: check if scrolled

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mScrollPosition += dy;


                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForPosition(0);
                if (vh != null) {
                    View podcastImage = recyclerView.findViewHolderForPosition(0).itemView.findViewById(R.id.epi_pod_image);

                    // Parallax animation of header
                    parallaxImage(podcastImage);

                    // Toollbar Shizzle
                    int triggerPos = podcastImage.getHeight();
                    checkToolbarVisibility(triggerPos);
                }


            }
        });


        return view;

    }

    private void checkToolbarVisibility(int triggerPos) {
        if(mScrollPosition > triggerPos && !mIsScrolledDown) {
            changeToolbar(mIsScrolledDown);
            mIsScrolledDown = true;

        } else if (mScrollPosition <= triggerPos && mIsScrolledDown) {
            changeToolbar(mIsScrolledDown);
            mIsScrolledDown = false;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    private void receive(Bundle args) {
        String podcastUuid = args.getString(MainActivity.ARGS_PODCAST_UUID);
        mPodcast = mRealm.where(Podcast.class).equalTo("uuid", podcastUuid).findFirst();
        mEpisodes = mRealm.where(Episode.class).equalTo("podcast.uuid", mPodcast.getUuid()).findAllSorted("pubdate", false);
    }

    private void parallaxImage(View view) {
        Rect rect = new Rect();
        view.getLocalVisibleRect(rect);
        if (lastTopValueAssigned != rect.top) {
            lastTopValueAssigned = rect.top;
            view.setY((float) (rect.top / 2.0));
        }
    }

    @Override
    public void onPlayPauseClick(int position) {

    }

    @Override
    public void onItemClick(int position) {
        Episode episode = mEpisodes.get(position);

        String episodeGuid = episode.getUuid();

        Bundle args = new Bundle();
        args.putString(MainActivity.ARGS_EPISODE_UUID, episodeGuid);

        EpisodeDetailsFragment episodeDetailsFragment = new EpisodeDetailsFragment();
        episodeDetailsFragment.setArguments(args);

        ((MainActivity) getActivity()).relpaceFragment(episodeDetailsFragment);
    }

    public void setColor(int color) {
        mColor = color;
    }

    private void changeToolbar(boolean isUp) {
        Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_in);
        Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fade_out);
        if (isUp) {
            mShadow.setAnimation(fadeIn);
            mShadow.setVisibility(View.VISIBLE);

            mToolbar.setTitle("");
            animateStatusBarBackgroundColor(mColor, Color.parseColor("#00000000"));
        } else {
            mShadow.startAnimation(fadeOut);
            mShadow.setVisibility(View.INVISIBLE);

            mToolbar.setTitle(mPodcast.getTitle());
            animateStatusBarBackgroundColor(Color.parseColor("#00000000"), mColor);
        }
    }

    private void animateStatusBarBackgroundColor(int fromColor, int toColor) {
        ValueAnimator backgroundAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mToolbar.setBackgroundColor((Integer) animation.getAnimatedValue());
            }
        });
        backgroundAnimator.start();
    }

    private void getColorFromImage() {
        ((EpisodesAdapter) mAdapter).setOnEpisodeAdapterHeaderListener(new EpisodesAdapter.EpisodeAdapterHeaderListener() {
            @Override
            public void onBound(ImageView image) {
                Bitmap podcastImage = ((BitmapDrawable) image.getDrawable()).getBitmap();
                Palette.generateAsync(podcastImage, new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        mColor = palette.getDarkVibrantColor(Color.parseColor(getString(R.color.dark_grey)));
                        int darkColor = StyleHelper.darkenColor(mColor, 0.85f);
                        ((MainActivity) getActivity()).setStatusBarTint(mColor);
                    }
                });
            }
        });
    }
}
