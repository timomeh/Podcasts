package de.timomeh.podcasts.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.models.Podcast;
import de.timomeh.podcasts.utils.StringHelper;
import de.timomeh.podcasts.utils.StyleHelper;
import io.realm.Realm;

/**
 * Created by Timo Maemecke (@timomeh) on 29/01/15.
 * <p/>
 * TODO: Add a class header comment
 */
public class EpisodesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface EpisodeAdapterListener {
        public void onPlayPauseClick(int position);
        public void onItemClick(int position);
    }

    public interface EpisodeAdapterHeaderListener {
        public void onBound(ImageView image);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private Context mContext;
    private List<Episode> mEpisodes;
    private Podcast mPodcast;
    private EpisodeAdapterListener mListener;
    private EpisodeAdapterHeaderListener mHeaderListener;

    public EpisodesAdapter(Context context, List<Episode> episodes, EpisodeAdapterListener listener) {
        mEpisodes = episodes;
        mPodcast = episodes.get(0).getPodcast();
        mContext = context;
        mListener = listener;
    }

    public static class ViewHolderHeader extends RecyclerView.ViewHolder {
        @InjectView(R.id.epi_pod_image) ImageView mPodcastImage;
        @InjectView(R.id.epi_pod_title) TextView mPodcastTitle;
        @InjectView(R.id.epi_pod_author) TextView mPodcastAuthor;

        public ViewHolderHeader(View v) {
            super(v);
            v.setTag(this);
            ButterKnife.inject(this, v);
        }
    }

    public static class ViewHolderItem extends RecyclerView.ViewHolder {
        @InjectView(R.id.epi_item) LinearLayout mEpisodeItem;
        @InjectView(R.id.epi_title) TextView mEpisodeTitle;
        @InjectView(R.id.epi_pubdate) TextView mEpisodeDate;
        @InjectView(R.id.epi_indicator) View mEpisodeIndicator;

        public ViewHolderItem(View v) {
            super(v);
            v.setTag(this);
            ButterKnife.inject(this, v);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_ITEM) {
            View v = LayoutInflater.from(parent.getContext())
                                   .inflate(R.layout.item_episode, parent, false);
            return new ViewHolderItem(v);
        } else if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                                   .inflate(R.layout.item_episode_header, parent, false);
            return new ViewHolderHeader(v);
        } else {
            throw new RuntimeException("There is no such Grid Item like " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof ViewHolderItem) {
            /**
             * Item View
             */
            final Episode episode = mEpisodes.get(position - 1);

            TextView title = ((ViewHolderItem) holder).mEpisodeTitle;
            TextView pubdate = ((ViewHolderItem) holder).mEpisodeDate;
            LinearLayout item = ((ViewHolderItem) holder).mEpisodeItem;
            View indicator = ((ViewHolderItem) holder).mEpisodeIndicator;

            // Click whole list item
            // Set episode seen and shoot da listener
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!episode.isSeen()) {
                        Realm realm = Realm.getInstance(mContext);
                        realm.beginTransaction();
                        episode.setSeen(true);
                        realm.commitTransaction();
                    }
                    mListener.onItemClick(position - 1);
                }
            });

            // Show/hide seen indicator
            if (episode.isSeen()) {
                indicator.setVisibility(View.GONE);
            }

            // Populate other views
            title.setText(episode.getTitle());
            pubdate.setText(StringHelper.humanDate(episode.getPubdate()));

        } else if (holder instanceof ViewHolderHeader) {
            /**
             * Header View
             */
            TextView title = ((ViewHolderHeader) holder).mPodcastTitle;
            TextView author = ((ViewHolderHeader) holder).mPodcastAuthor;
            ImageView image = ((ViewHolderHeader) holder).mPodcastImage;

            int imageDimen = StyleHelper.getWindowWidth(mContext);

            // Populate other views
            Picasso.with(mContext)
                    .load(mPodcast.getImageurl())
                    .resize(imageDimen, imageDimen)
                    .centerCrop()
                    .placeholder(R.drawable.podcast_placeholder)
                    .into(image);
            title.setText(mPodcast.getTitle());
            author.setText(mPodcast.getAuthor());
            mHeaderListener.onBound(image);
        } else {
            /**
             * Unknown View (should never happen)
             */
            throw new RuntimeException("Unknown instance of ViewHolder");
        }
    }

    @Override
    public int getItemCount() {
        return mEpisodes.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position)) {
            return TYPE_HEADER;
        }

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    public void refill(List<Episode> episodes) {
        mEpisodes.clear();
        mEpisodes.addAll(episodes);
        mPodcast = episodes.get(0).getPodcast();
        notifyDataSetChanged();
    }

    public void setOnEpisodeAdapterHeaderListener(EpisodeAdapterHeaderListener listener) {
        mHeaderListener = listener;
    }


}
