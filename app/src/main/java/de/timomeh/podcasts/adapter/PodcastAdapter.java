package de.timomeh.podcasts.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.timomeh.podcasts.R;
import de.timomeh.podcasts.models.Podcast;
import de.timomeh.podcasts.utils.StyleHelper;

/**
 * Created by Timo Maemecke (@timomeh) on 24/01/15.
 *
 * Adapter for Podcasts in a RecyclerView (using a GridListView)
 */
public class PodcastAdapter extends RecyclerView.Adapter<PodcastAdapter.ViewHolder> implements View.OnLongClickListener, View.OnClickListener {

    public interface PodcastAdapterListener {
        public void onClick(int position);
        public void onLongClick(int position);
    }

    PodcastAdapterListener mListener;

    private static final String TAG = PodcastAdapter.class.getSimpleName();
    private List<Podcast> mPodcasts;
    private Context mContext;

    public PodcastAdapter(Context context, List<Podcast> podcasts, PodcastAdapterListener listener) {
        mContext = context;
        mPodcasts = podcasts;
        mListener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.pod_grid_image) ImageView mPodcastImage;
        @InjectView(R.id.pod_grid_item) LinearLayout mPodcastItem;
        @InjectView(R.id.pod_grid_title) TextView mPodcastTitle;
        @InjectView(R.id.pod_grid_wrapper) RelativeLayout mPodcastWrapper;

        public ViewHolder(View v) {
            super(v);
            v.setTag(this);
            ButterKnife.inject(this, v);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext())
                               .inflate(R.layout.item_podcast, parent, false);

        v.setOnLongClickListener(PodcastAdapter.this);
        v.setOnClickListener(PodcastAdapter.this);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int i) {

        // Set padding of grid manually
        int twoDp = (int) StyleHelper.convertDpToPixel(2, mContext);
        if (i % 2 == 0) {
            holder.mPodcastWrapper.setPadding(0, 0, twoDp, twoDp * 2);
        } else {
            holder.mPodcastWrapper.setPadding(twoDp, 0, 0, twoDp*2);
        }

        holder.mPodcastTitle.setText(mPodcasts.get(i).getTitle());

        int imageDimen = StyleHelper.getWindowWidth(mContext) / 2;

        Picasso.with(mContext)
                .load(mPodcasts.get(i).getImageurl())
                .resize(imageDimen, imageDimen)
                .centerCrop()
                .placeholder(R.drawable.podcast_placeholder)
                .into(holder.mPodcastImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Bitmap podcastImage = ((BitmapDrawable) holder.mPodcastImage.getDrawable()).getBitmap();
                        Palette.generateAsync(podcastImage, new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                String defaultColor = "#ffffff";
                                int backgroundColor = palette.getDarkVibrantColor(Color.parseColor(defaultColor));
                                int textColor = StyleHelper.isBrightColor(backgroundColor) ? Color.parseColor("#000000") : Color.parseColor("#ffffff");
                                holder.mPodcastItem.setBackgroundColor(backgroundColor);
                                holder.mPodcastTitle.setTextColor(textColor);
                            }
                        });
                    }

                    @Override
                    public void onError() {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return mPodcasts.size();
    }

    public void refill(List<Podcast> podcasts) {
        mPodcasts = null;
        mPodcasts = podcasts;
        notifyDataSetChanged();
    }

    public void add(int pos, Podcast podcast) {
        mPodcasts.add(pos, podcast);
        notifyDataSetChanged();
    }

    @Override
    public boolean onLongClick(View v) {
        ViewHolder holder = (ViewHolder) v.getTag();
        mListener.onLongClick(holder.getPosition());

        return false;
    }


    @Override
    public void onClick(View v) {
        ViewHolder holder = (ViewHolder) v.getTag();
        mListener.onClick(holder.getPosition());
    }

}
