package de.timomeh.podcasts.manager;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.OkHeaders;

import org.xml.sax.Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.timomeh.podcasts.models.Episode;
import de.timomeh.podcasts.models.Podcast;


/**
 * Created by Timo Maemecke (@timomeh) on 13/02/15.
 * <p/>
 * Factory Class for fetching contents of a syndication feed and parse it as a Podcast Object.
 */
public class SyndicationFeed {

    private static final String TAG = SyndicationFeed.class.getSimpleName();
    public static final String ITUNES_DTD = "http://www.itunes.com/dtds/podcast-1.0.dtd";
    public static final String RFC_DATE = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String RESULT_PODCAST = "podcast";
    public static final String RESULT_EPISODES = "episodes";

    public interface OnResponseListener {
        void onSuccess(Headers headers);
        void onNotModified();
        void onFailure();
    }

    public interface OnBuildListener {
        void onBuild(Podcast podcast, List<Episode> episodes);
    }

    private URL mUrl;
    private InputStream mInputStream;
    private String mLastModified;
    private String mEtag;
    private int mLimit = -1;
    private int mSkip = 0;

    private OnResponseListener mOnResponseListener = new OnResponseListener() {
        @Override
        public void onSuccess(Headers headers) {

        }

        @Override
        public void onNotModified() {

        }

        @Override
        public void onFailure() {

        }
    };
    private OnBuildListener mOnBuildListener = new OnBuildListener() {
        @Override
        public void onBuild(Podcast podcast, List<Episode> episodes) {

        }
    };

    public SyndicationFeed(URL url) {
        mUrl = url;
    }

    public void setOnResponseListener(OnResponseListener listener) {
        mOnResponseListener = listener;
    }

    public void setOnBuildListener(OnBuildListener listener) {
        mOnBuildListener = listener;
    }

    /**
     * Make async GET Request to URL.
     * @param headers Request Headers
     */
    public void get(Headers headers) {
        request(headers, false);
    }

    public void head(Headers headers) {
        request(headers, true);
    }

    private void request(Headers headers, final boolean onlyHead) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(mUrl)
                .headers(headers);
        if (onlyHead) requestBuilder.head();
        Request request = requestBuilder.build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                mOnResponseListener.onFailure();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 304) {
                    mOnResponseListener.onNotModified();
                }
                else if (response.isSuccessful()) {
                    mOnResponseListener.onSuccess(response.headers());
                    if (!onlyHead) {
                        mInputStream = response.body().byteStream();
                        Map<String, Object> resultMap = build();
                        Podcast podcast = null;
                        List<Episode> episodes = null;

                        if (resultMap.get(RESULT_PODCAST) != null) podcast = (Podcast) resultMap.get(RESULT_PODCAST);
                        if (resultMap.get(RESULT_EPISODES) != null) episodes = (List<Episode>) resultMap.get(RESULT_EPISODES);
                        mOnBuildListener.onBuild(podcast, episodes);
                    }
                } else {
                    mOnResponseListener.onFailure();
                }
            }
        });
    }

    /**
     * Limit amount of items.
     * @param limit Amount of items to limit
     * @return current Object
     */
    public SyndicationFeed setLimit(int limit) {
        mLimit = limit;
        return this;
    }

    /**
     * Set amount if items to skip.
     * @param skip Amount of items to skip.
     * @return current Object
     */
    public SyndicationFeed setSkip(int skip) {
        mSkip = skip;
        return this;
    }

    /**
     * Parse the whole feed line by line with SAX. Skip and Limit will be respected.
     * @return Map with Podcast and Episode. Keys are Constants RESULT_PODCAST and RESULT_EPISODES.
     */
    public Map<String, Object> build() {
        RootElement root = new RootElement("rss");
        final Podcast podcast = new Podcast();
        final Episode episode = new Episode();
        final List<Episode> episodes = new ArrayList<>();
        final boolean[] ignoreCurrentItem = {false};
        final int[] itemCount = {0};

        // Parse channel data
        Element channel = root.getChild("channel");
        podcast.setFeedurl(mUrl.toString());
        channel.getChild("title").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setTitle(body);
            }
        });
        channel.getChild(ITUNES_DTD, "author").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setAuthor(body);
            }
        });
        channel.getChild(ITUNES_DTD, "image").setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                if (attributes.getValue("href") != null) podcast.setImageurl(attributes.getValue("href"));
            }
        });
        channel.getChild("link").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setWeburl(body);
            }
        });
        channel.getChild(ITUNES_DTD, "subtitle").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setSubtitle(body);
            }
        });
        channel.getChild(ITUNES_DTD, "summary").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setSummary(body);
            }
        });
        channel.getChild(ITUNES_DTD, "explicit").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setExplicit("yes".equalsIgnoreCase(body));
            }
        });
        channel.getChild(ITUNES_DTD, "new-feed-url").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                podcast.setFeedurl(body);
            }
        });


        // Parse Item Data
        Element item = channel.getChild("item");
        item.setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                // Reset episode data
                episode.setGuid("");
                episode.setTitle("");
                episode.setImageurl("");
                episode.setWeburl("");
                episode.setFileurl("");
                episode.setFileuri("");
                episode.setFilesize(0);
                episode.setDuration(0);
                episode.setSummary("");
                episode.setExplicit(false);
                episode.setPubdate(null);

                // Skip
                if (itemCount[0] < mSkip) {
                    ignoreCurrentItem[0] = true;
                } else {
                    ignoreCurrentItem[0] = false;

                    // Limit
                    if (mLimit != -1 && itemCount[0] >= mLimit + mSkip) {
                        ignoreCurrentItem[0] = true;
                    }
                }

                itemCount[0]++;
            }
        });
        item.setEndElementListener(new EndElementListener() {
            @Override
            public void end() {
                if (!ignoreCurrentItem[0]) {
                    // Use Fileurl as Guid when not provided
                    if (episode.getGuid() == null || episode.getGuid().equals("")) {
                        episode.setGuid(episode.getFileurl());
                    }
                    Episode clone = new Episode();
                    clone.setGuid(episode.getGuid());
                    clone.setTitle(episode.getTitle());
                    clone.setImageurl(episode.getImageurl());
                    clone.setWeburl(episode.getWeburl());
                    clone.setFileurl(episode.getFileurl());
                    clone.setFilesize(episode.getFilesize());
                    clone.setFiletype(episode.getFiletype());
                    clone.setDuration(episode.getDuration());
                    clone.setSummary(episode.getSummary());
                    clone.setExplicit(episode.isExplicit());
                    clone.setPubdate(episode.getPubdate());
                    episodes.add(clone);
                }
            }
        });
        item.getChild("guid").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setGuid(body);
            }
        });
        item.getChild("title").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setTitle(body);
            }
        });
        item.getChild(ITUNES_DTD, "image").setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                if (attributes.getValue("href") != null) episode.setImageurl(attributes.getValue("href"));
            }
        });
        item.getChild("link").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setWeburl(body);
            }
        });
        item.getChild("enclosure").setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                if (attributes.getValue("url") != null) episode.setFileurl(attributes.getValue("url"));
                if (attributes.getValue("length") != null) episode.setFilesize(Long.parseLong(attributes.getValue("length")));
                if (attributes.getValue("type") != null) episode.setFiletype(attributes.getValue("type"));
            }
        });
        item.getChild(ITUNES_DTD, "duration").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setDuration(convertDuration(body));
            }
        });
        item.getChild(ITUNES_DTD, "summary").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setSummary(body);
            }
        });
        item.getChild(ITUNES_DTD, "explicit").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                episode.setExplicit("yes".equalsIgnoreCase(body));
            }
        });
        item.getChild("pubDate").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                Date pubDate = null;
                try {
                    pubDate = parseDate(body);
                } catch (ParseException e) {
                    // Intentionally left blank
                }
                episode.setPubdate(pubDate);
            }
        });

        Map<String, Object> result = null;

        try {
            Xml.parse(mInputStream, Xml.Encoding.UTF_8, root.getContentHandler());
            result = new HashMap<>();
            result.put(RESULT_PODCAST, podcast);
            result.put(RESULT_EPISODES, episodes);
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
            // Intentionally no exception handling
        }

        return result;
    }


    /**
     * Parse a RFC 2282 String with format EEE, dd MMM yyyy HH:mm:ss Z
     * @param date Date representation
     * @return Parsed Date
     * @throws ParseException
     */
    protected static Date parseDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(RFC_DATE, Locale.US);
        return format.parse(date);
    }

    /**
     * Parse different time length formats like HH:mm:ss, mm:ss and only seconds
     * @param duration Length representation
     * @return Seconds
     */
    protected static long convertDuration(String duration) {
        String[] durHelper = duration.split(":");
        long dur;
        if (durHelper.length == 1) {
            // given in seconds
            dur = Long.parseLong(duration, 10);
        } else if (durHelper.length == 2) {
            // given in mm:ss
            dur = Integer.parseInt(durHelper[0]) * 60 + Integer.parseInt(durHelper[1]);
        } else {
            // given in hh:mm:ss (more colons are ignored)
            // see how apple handles this https://www.apple.com/de/itunes/podcasts/specs.html#duration
            dur = (Integer.parseInt(durHelper[0]) * 60 + Integer.parseInt(durHelper[1])) * 60
                    + Integer.parseInt(durHelper[2]);
        }

        return dur;
    }
}
