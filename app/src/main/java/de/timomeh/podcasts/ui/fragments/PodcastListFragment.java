package de.timomeh.podcasts.ui.fragments;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.adapter.PodcastAdapter;
import de.timomeh.podcasts.manager.PodcastManager;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.models.Podcast;
import de.timomeh.podcasts.ui.activities.MainActivity;
import io.realm.Realm;

/**
 * Created by Timo Maemecke (@timomeh) on 29/01/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class PodcastListFragment extends Fragment implements PodcastAdapter.PodcastAdapterListener {

    public PodcastListFragment() { }

    private static final String TAG = PodcastListFragment.class.getSimpleName();

    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Toolbar mToolbar;
    private List<Podcast> mPodcasts;
    private Realm mRealm;

    @InjectView(R.id.swipe_refresh) SwipeRefreshLayout mSwipeRefreshLayout;
    @InjectView(R.id.recycler_view) RecyclerView mRecyclerView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mRealm = Realm.getInstance(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_podcast_list, container, false);
        ButterKnife.inject(this, view);

        ((MainActivity) getActivity()).setStatusBarTint(Color.parseColor("#1a1a1a"));

        mToolbar = (Toolbar) getActivity().findViewById(R.id.main_toolbar);

        mToolbar.setBackgroundColor(getResources().getColor(R.color.dark_grey));
        mToolbar.setTitle("Podcasts");

        retreivePodcasts();

        // improved performance of recycler view
        mRecyclerView.setHasFixedSize(true);

        // use grid layout
        mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // set adapter
        mAdapter = new PodcastAdapter(getActivity(), mPodcasts, this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        final MenuItem addItem = menu.findItem(R.id.action_add);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        SearchableInfo searchInfo = searchManager.getSearchableInfo(getActivity().getComponentName());
        searchView.setSearchableInfo(searchInfo);


        addItem.setOnMenuItemClickListener(mOnMenuAddPodcastClickListener);
    }

    @Override
    public void onClick(int position) {
        String podcastUuid = mPodcasts.get(position).getUuid();

        Bundle args = new Bundle();
        args.putString(MainActivity.ARGS_PODCAST_UUID, podcastUuid);

        EpisodeListFragment episodeListFragment = new EpisodeListFragment();
        episodeListFragment.setArguments(args);

        ((MainActivity) getActivity()).relpaceFragment(episodeListFragment);
    }

    @Override
    public void onLongClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Remove?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Podcast podcastToDelete = mPodcasts.get(position);
                        PodcastManager manag = new PodcastManager(getActivity(), podcastToDelete);
                        manag.delete();

                        mPodcasts = mRealm.where(Podcast.class).findAllSorted("title");
                        ((PodcastAdapter) mRecyclerView.getAdapter()).refill(mPodcasts);
                    }
                })
                .setNegativeButton("No", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private MenuItem.OnMenuItemClickListener mOnMenuAddPodcastClickListener = new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_subscribe, null);
            final EditText url = (EditText) view.findViewById(R.id.pod_url);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Add Podcast by URL");
            builder.setView(view);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialog, final int which) {
                    Podcast podcast = new Podcast();
                    podcast.setFeedurl(url.getText().toString());

                    PodcastManager manager = new PodcastManager(getActivity(), podcast);
                    manager.save();
                    manager.setOnSaveListener(new PodcastManager.OnSaveListener() {
                        @Override
                        public void onSave(final Podcast newPodcast) {
                            finishRefresh();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "Subscribed to " + newPodcast.getTitle() + ".", Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure() {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), "Failed.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", null);

            final AlertDialog dialog = builder.create();

            // "hack" for popping up softkeyboard directly
            url.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    }
                }
            });
            url.requestFocus();

            dialog.show();
            return false;
        }
    };

    private void retreivePodcasts() {
        Realm realm = Realm.getInstance(getActivity());
        mPodcasts = realm.where(Podcast.class).findAllSorted("title");
    }

    private void refreshPodcasts() {
        retreivePodcasts();
        if (mPodcasts != null && mPodcasts.size() != 0) {
            final int[] updated = {0};
            for (final Podcast podcast : mPodcasts) {
                PodcastManager manager = new PodcastManager(getActivity(), podcast);
                manager.update();
                manager.setOnUpdateListener(new PodcastManager.OnUpdateListener() {
                    @Override
                    public void onUpdated(final boolean changed) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, podcast.getTitle() + " has changed? " + changed);
                            }
                        });
                        updated[0]++;
                        if (updated[0] == mPodcasts.size()) {
                            finishRefresh();
                        }
                    }

                    @Override
                    public void onFailure() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Couldn't update " + podcast.getTitle() + ".", Toast.LENGTH_LONG).show();
                            }
                        });
                        updated[0]++;
                        if (updated[0] == mPodcasts.size()) {
                            finishRefresh();
                        }
                    }
                });
            }
        } else {
            finishRefresh();
        }
    }

    private void finishRefresh() {
        Handler mainHandler = new Handler(getActivity().getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Realm realm = Realm.getInstance(getActivity());
                mPodcasts = realm.where(Podcast.class).findAllSorted("title");

                ((PodcastAdapter) mRecyclerView.getAdapter()).refill(mPodcasts);

                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        };
        mainHandler.post(runnable);
    }

    protected SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshPodcasts();
        }
    };
}
