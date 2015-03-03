package de.timomeh.podcasts.ui.activities;

import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.services.PlayerService;
import de.timomeh.podcasts.ui.fragments.EpisodeDetailsFragment;
import de.timomeh.podcasts.ui.fragments.EpisodeListFragment;
import de.timomeh.podcasts.ui.fragments.PlayerFragment;
import de.timomeh.podcasts.ui.fragments.PodcastListFragment;

/**
 * Created by Timo Maemecke (@timomeh) on 24/01/15.
 * <p/>
 * Show a List of subscribed Podcasts (inflates PodcastListFragment)
 */
public class MainActivity extends ActionBarActivity implements FragmentManager.OnBackStackChangedListener {

    public static final String ARGS_EPISODE_UUID = "episodeUuid";
    public static final String ARGS_PODCAST_UUID = "podcastUuid";


    private int mCurrentStatusBarColor;
    private int mPanelHeight;

    @InjectView(R.id.main_toolbar) Toolbar mToolbar;
    @InjectView(R.id.slide_panel) SlidingUpPanelLayout mSlidePanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        setSupportActionBar(mToolbar);
        displayBackButton();

        PlayerFragment playerFragment = null;

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            fm.addOnBackStackChangedListener(this);
            PodcastListFragment podcastListFragment = new PodcastListFragment();
            podcastListFragment.setArguments(getIntent().getExtras());
            fm.beginTransaction().add(R.id.fragment_container, podcastListFragment).commit();

            playerFragment = new PlayerFragment();
            fm.beginTransaction().add(R.id.fragment_player, playerFragment).commit();
        } else {
            playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_player);
        }


        mPanelHeight = mSlidePanel.getPanelHeight();
        if (playerFragment.getEpisode() == null) {
            mSlidePanel.setPanelHeight(0);
        }

        mSlidePanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {

            }

            @Override
            public void onPanelCollapsed(View view) {
                SystemBarTintManager manager = new SystemBarTintManager(MainActivity.this);
                manager.setStatusBarTintEnabled(true);
                manager.setStatusBarTintColor(mCurrentStatusBarColor);
            }

            @Override
            public void onPanelExpanded(View view) {
                SystemBarTintManager manager = new SystemBarTintManager(MainActivity.this);
                PlayerFragment playerFragment = (PlayerFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_player);
                manager.setStatusBarTintEnabled(true);
                manager.setStatusBarTintColor(playerFragment.getStatusBarColor());
            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if(mSlidePanel.getPanelState().equals(SlidingUpPanelLayout.PanelState.EXPANDED)) {
            mSlidePanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
        displayBackButton();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    /**
     * Will show the BackButton in the Toolbar, if there is something to go back to
     */
    public void displayBackButton() {
        boolean iCanGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(iCanGoBack);
    }

    public void relpaceFragment(Fragment newFragment) {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.fragment_container, newFragment);
        t.addToBackStack(newFragment.getClass().getName());
        t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        t.commit();
    }

    public void setStatusBarTint(int color) {
        mCurrentStatusBarColor = color;
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintColor(color);
    }

    public void showPanel() {
        if (mSlidePanel.getPanelHeight() != mPanelHeight) {
            ValueAnimator slidePanelAnimator = ValueAnimator.ofObject(new IntEvaluator(), 0, mPanelHeight);
            slidePanelAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mSlidePanel.setPanelHeight((Integer) animation.getAnimatedValue());
                }
            });
            slidePanelAnimator.start();
        }
    }
}
